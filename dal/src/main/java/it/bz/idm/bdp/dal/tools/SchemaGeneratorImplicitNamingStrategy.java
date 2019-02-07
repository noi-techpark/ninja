/**
 * BDP data - Data Access Layer for the Big Data Platform
 * Copyright © 2018 IDM Südtirol - Alto Adige (info@idm-suedtirol.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program (see LICENSES/GPL-3.0.txt). If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0
 */
package it.bz.idm.bdp.dal.tools;

import java.util.List;
import java.util.Locale;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitForeignKeyNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.hibernate.boot.spi.MetadataBuildingContext;

public class SchemaGeneratorImplicitNamingStrategy extends ImplicitNamingStrategyComponentPathImpl {

	private static final long serialVersionUID = -2837008678932539996L;

	@Override
    protected Identifier toIdentifier(String stringForm, MetadataBuildingContext buildingContext) {
		System.out.println("NAME=" + stringForm);
        return super.toIdentifier(lowerSnakecase(stringForm), buildingContext);
    }

	@Override
	public Identifier determineUniqueKeyName(ImplicitUniqueKeyNameSource source) {
//		Identifier userProvidedIdentifier = source.getUserProvidedIdentifier();
//		if (userProvidedIdentifier != null)
//			return userProvidedIdentifier;

		System.out.println("NAME_UC=" + source.getTableName());

		if ("type".equalsIgnoreCase(source.getTableName().toString())) {
			System.out.println("XXXXX");
		}

		String cols = identifiersToSnakeCase(source.getColumnNames());
		String name = sanitizeName("uc_" + source.getTableName() + cols);
		return toIdentifier(name, source.getBuildingContext());
	}

	@Override
	public Identifier determineForeignKeyName(ImplicitForeignKeyNameSource source) {
//		Identifier userProvidedIdentifier = source.getUserProvidedIdentifier();
//		if (userProvidedIdentifier != null) {
//			System.out.println("NAMEXXX=" + userProvidedIdentifier.getText());
//			return userProvidedIdentifier;
//		}

		String cols = identifiersToSnakeCase(source.getColumnNames());
		String colsRef = identifiersToSnakeCase(source.getReferencedColumnNames());
		String name = sanitizeName("fk_" + source.getTableName() + cols + "_" + source.getReferencedTableName() + colsRef);
		return toIdentifier(name, source.getBuildingContext());
	}

	/**
	 * Translate camel-case strings into lower-case strings with underscores.
	 * @param name
	 * @return
	 */
    private static String lowerSnakecase(String name) {
        final StringBuilder buf = new StringBuilder(name.replace('.', '_'));
        for (int i = 1; i < buf.length() - 1; i++) {
            if (Character.isLowerCase(buf.charAt(i - 1))
                    && Character.isUpperCase(buf.charAt(i))
                    && Character.isLowerCase(buf.charAt(i + 1))) {
                buf.insert(i++, '_');
            }
        }
        return buf.toString().toLowerCase(Locale.ROOT);
    }

    private static String identifiersToSnakeCase(List<Identifier> identifiers) {
    	String cols = "";
		for (Identifier col : identifiers) {
			cols += "_" + col;
		}
		return cols;
    }

    /**
	 * PostgreSQL supports max. lengths of 63 characters. It would truncate
	 * the end of the identifier name, but we do it better in the middle and add another
	 * underscore at the end to signal that it is shortened.
     * @param name
     * @return
     */
    private static String sanitizeName(String name) {
    	if (name.length() <= 63)
    		return name;

    	return name.substring(0, 31) + "_" + name.substring(name.length() - 30) + "_";
    }
}
