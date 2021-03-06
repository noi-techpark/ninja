package it.bz.idm.bdp.ninja.utils.resultbuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import it.bz.idm.bdp.ninja.utils.querybuilder.Schema;
import it.bz.idm.bdp.ninja.utils.querybuilder.Target;
import it.bz.idm.bdp.ninja.utils.simpleexception.ErrorCodeInterface;
import it.bz.idm.bdp.ninja.utils.simpleexception.SimpleException;

public class ResultBuilder {

	public enum ErrorCode implements ErrorCodeInterface {
		RESPONSE_SIZE("Response size of %d MB exceeded. Please rephrase your request. Use a flat representation, WHERE, SELECT, LIMIT with OFFSET or a narrow time interval."),
		WRONG_TREE_BUILDING_KEY_TYPE("The column '%s' used to build the TREE representation must be of type STRING");

		private final String msg;

		ErrorCode(final String msg) {
			this.msg = msg;
		}

		@Override
		public String getMsg() {
			return "TREE BUILDING: " + msg;
		}
	}

	public static int calculateLevel(Map<String, Object> rec, List<String> hierarchy, List<String> prevValues, List<String> currValues) {

		if (prevValues.isEmpty()) {
			for (int i = 0; i < hierarchy.size(); i++) {
				prevValues.add("");
			}
		}

		currValues.clear();
		int i = 0;
		boolean levelSet = false;
		int renewLevel = hierarchy.size();
		for (String colname : hierarchy) {
			String value = (String) rec.get(colname);
			if (value == null) {
				throw new RuntimeException(colname + " not found in select. Unable to build hierarchy.");
			}
			currValues.add(value);
			if (!levelSet && !value.equals(prevValues.get(i))) {
				renewLevel = i;
				levelSet = true;
			}
			i++;
		}
		return renewLevel;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> build(String entryPoint, String exitPoint, boolean showNull, List<Map<String, Object>> queryResult, Schema schema, int maxAllowedSizeInMB) {
		AtomicLong size = new AtomicLong(0);
		long maxAllowedSize = maxAllowedSizeInMB > 0 ? maxAllowedSizeInMB * 1000000 : 0;

		if (queryResult == null || queryResult.isEmpty()) {
			return new HashMap<>();
		}

		List<String> currValues = new ArrayList<>();
		List<String> prevValues = new ArrayList<>();
		Map<String, List<Target>> catalog = new HashMap<>();
		Map<String, Object> result = new HashMap<>();

		// Should be present inside the definition, just entrypoint needed
		List<List<String>> hierarchy = new ArrayList<>(schema.getHierarchy(entryPoint, exitPoint));
		List<String> hierarchyTriggerKeys = new ArrayList<>(schema.getHierarchyTriggerKeys(entryPoint, exitPoint));
		int maxLevel = hierarchy.size() - 1;

		Map<String, Map<String, Object>> cache = new HashMap<>();
		Map<String, Object> firstResultRecord = queryResult.get(0);

		for (String key : hierarchyTriggerKeys) {
			if (firstResultRecord.get(key) instanceof String)
				continue;

			throw new SimpleException(ErrorCode.WRONG_TREE_BUILDING_KEY_TYPE, key);
		}


		// create catalog of Targets, since each record in this result set contains exactly the same names
		for (List<String> targetDefListNames : hierarchy) {
			for (String targetDefListName : targetDefListNames) {
				Set<String> targetDefNames = schema.getOrNull(targetDefListName).getFinalNames();
				List<Target> currentTargetList = new ArrayList<>();
				for (String targetName : firstResultRecord.keySet()) {
					Target target = new Target(targetName);
					if (targetDefNames.contains(target.getName())) {
						currentTargetList.add(target);
						catalog.putIfAbsent(targetDefListName, currentTargetList);
						cache.putIfAbsent(targetDefListName, new HashMap<>());
					}
				}
			}
		}

		// We should check for all these prerequisites before starting the record loop to generate the result set
		// we can also limit the possible levels, if we see that it stops always at 2 (for datatypes, not here in edges, just an example)
		// and that the first two levels are mandatory, so it must never be lower than those

		for (Map<String, Object> rec : queryResult) {

			int renewLevel = calculateLevel(rec, hierarchyTriggerKeys, prevValues, currValues);

			for (int level = renewLevel; level <= maxLevel; level++) {
				for (String targetDefListName : hierarchy.get(level)) {
					Map<String, Object> curObject = makeObj(catalog.get(targetDefListName), rec, showNull, size);
					cache.put(targetDefListName, curObject);
				}
			}

			for (int level = maxLevel; level >= renewLevel; level--) {
				for (String targetDefListName : hierarchy.get(level)) {
					Map<String, Object> curObject = cache.get(targetDefListName);
					if (curObject == null || curObject.isEmpty()) {
						continue;
					}
					LookUp lookup = schema.get(targetDefListName).getLookUp();
					Map<String, Object> parent = cache.get(lookup.getParentDefListName());
					if (parent == null) {
						parent = result;
					}
					String mapTypeValue = (String) rec.get(lookup.getMapTypeKey());
					switch (lookup.getType()) {
						case INLINE:
							parent.put(lookup.getParentTargetName(), curObject);
							break;
						case MERGE:
							parent.put(lookup.getParentTargetName(), curObject.get(lookup.getParentTargetName()));
							break;
						case MAP:
							if (lookup.getParentTargetName() == null) {
								parent.put(mapTypeValue, curObject);
								break;
							}

							Map<String, Object> parentSub = (Map<String, Object>) parent.getOrDefault(lookup.getParentTargetName(), new TreeMap<>());
							if (parentSub.isEmpty()) {
								parent.put(lookup.getParentTargetName(), parentSub);
								parentSub.put(mapTypeValue, curObject);
							} else {
								parentSub.putIfAbsent(mapTypeValue, curObject);
							}

							break;
						case LIST:
							List<Object> newList = (List<Object>) parent.getOrDefault(lookup.getParentTargetName(), new ArrayList<>());
							if (newList.isEmpty()) {
								parent.put(lookup.getParentTargetName(), newList);
							}
							newList.add(curObject);
							break;
					}
				}
			}

			prevValues.clear();
			prevValues.addAll(currValues);

			if (maxAllowedSize > 0 && maxAllowedSize < size.get()) {
				throw new SimpleException(ErrorCode.RESPONSE_SIZE, maxAllowedSizeInMB);
			}
		}
		return result;
	}

	private static Map<String, Object> makeObj(List<Target> targetCatalog, Map<String, Object> record, boolean showNull, AtomicLong sizeEstimate) {

		if (targetCatalog == null || targetCatalog.isEmpty() || record == null || record.isEmpty()) {
			return new TreeMap<>();
		}

		Map<String, Object> result = new TreeMap<>();
		int size = 0;

		for (Target target : targetCatalog) {
			Object cellData = record.get(target.getName());

			if (!showNull && cellData == null)
				continue;

			if (target.hasJson()) {
				@SuppressWarnings("unchecked")
				Map<String, Object> jsonObj = (Map<String, Object>) result.getOrDefault(target.getName(), new TreeMap<>());
				jsonObj.put(target.getJson(), cellData);
				size += target.getJson().length();
				if (jsonObj.size() == 1) {
					result.put(target.getName(), jsonObj);
					size += target.getName().length();
				}
			} else {
				result.put(target.getName(), cellData);
				size += target.getName().length();
			}
			size += cellData == null ? 0 : cellData.toString().length();
		}

		sizeEstimate.getAndAdd(size);
		return result;
	}
}
