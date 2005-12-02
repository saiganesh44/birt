/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.model.util;

import java.util.List;

import org.eclipse.birt.report.model.api.metadata.IElementPropertyDefn;
import org.eclipse.birt.report.model.api.metadata.PropertyValueException;
import org.eclipse.birt.report.model.api.util.StringUtil;
import org.eclipse.birt.report.model.core.Module;
import org.eclipse.birt.report.model.core.ReferencableStructure;
import org.eclipse.birt.report.model.core.Structure;
import org.eclipse.birt.report.model.elements.Library;
import org.eclipse.birt.report.model.metadata.PropertyDefn;
import org.eclipse.birt.report.model.metadata.PropertyType;
import org.eclipse.birt.report.model.metadata.StructRefValue;
import org.eclipse.birt.report.model.metadata.StructureDefn;

/**
 * Utility class for the StructRefPropertyType.
 */

public class StructureRefUtil
{

	/**
	 * Looks up the target structure with the given name.
	 * 
	 * @param module
	 *            the module in which to search the target locally
	 * @param targetDefn
	 *            the definition for the target structure
	 * @param name
	 *            the name of the target structure to search
	 * @return the structure with the given name in the module locally,
	 *         otherwise null
	 */

	public static Structure findNativeStructure( Module module,
			StructureDefn targetDefn, String name )
	{
		if ( StringUtil.isBlank( name ) || targetDefn == null )
			return null;

		IElementPropertyDefn defn = module
				.getReferencablePropertyDefn( targetDefn.getName( ) );

		if ( defn == null )
			return null;
		assert defn.getTypeCode( ) == PropertyType.STRUCT_TYPE;

		if ( defn.isList( ) )
		{
			List list = module.getListProperty( module, defn.getName( ) );
			if ( list == null )
				return null;
			for ( int i = 0; i < list.size( ); i++ )
			{
				Structure struct = (Structure) list.get( i );
				if ( name.equals( struct.getReferencableProperty( ) ) )
					return struct;
			}
		}
		else
		{
			Structure struct = (Structure) module.getProperty( module, defn
					.getName( ) );
			if ( name.equals( struct.getReferencableProperty( ) ) )
				return struct;
		}
		return null;
	}

	/**
	 * Looks up the target structure with the given name in the module and its
	 * directly including libraries.
	 * 
	 * @param module
	 *            the module in which to search the target locally
	 * @param targetDefn
	 *            the definition for the target structure
	 * @param name
	 *            the name of the target structure to search
	 * @return the structure with the given name in the module locally,
	 *         otherwise null
	 */

	public static Structure findStructure( Module module,
			StructureDefn targetDefn, String name )
	{
		if ( StringUtil.isBlank( name ) || targetDefn == null || module == null )
			return null;

		String namespace = StringUtil.extractNamespace( name );
		String structName = StringUtil.extractName( name );
		Module moduleToSearch = module;
		if ( namespace != null )
		{
			moduleToSearch = module.getLibraryWithNamespace( namespace );
		}

		if ( moduleToSearch != null )
			return findNativeStructure( moduleToSearch, targetDefn, structName );
		return null;

	}

	/**
	 * Resolves the structure with the given name.
	 * 
	 * @param module
	 *            report design
	 * @param defn
	 *            the definition of the property or member to resolve
	 * @param name
	 *            structure name
	 * @return the resolved structure reference value
	 */

	public static StructRefValue resolve( Module module, PropertyDefn defn,
			String name )
	{
		if ( StringUtil.isBlank( name ) || defn == null || module == null )
			return null;

		assert defn.getTypeCode( ) == PropertyType.STRUCT_REF_TYPE;
		StructureDefn targetDefn = (StructureDefn) defn.getStructDefn( );
		assert targetDefn != null;

		Structure target = null;

		// the module which the target structure lies

		Module targetModule = null;

		// if the property is a structure reference like "imageName", then the
		// name should not contain "namespace" prefix

		// TODO: the embeddedImage has "." in the name which will cause the
		// nemaspace ambiguity.

		if ( ReferencableStructure.LIB_REFERENCE_MEMBER
				.equals( defn.getName( ) ) )
		{
			String structName = StringUtil.extractName( name );
			String namespace = StringUtil.extractNamespace( name );
			targetModule = module.getLibraryWithNamespace( namespace );
			if ( targetModule != null )
			{
				target = findNativeStructure( targetModule, targetDefn, structName );
				if ( target != null )
					return new StructRefValue( namespace, target );
			}

			// if the target module is null or target structure is null, then
			// value is unresolved

			return new StructRefValue( namespace, structName );
		}
		else
		{
			// for now, "imageName" can only refer a local embedded image

			targetModule = module;
			target = findNativeStructure( targetModule, targetDefn, name );
			String namespace = targetModule instanceof Library
					? ( (Library) targetModule ).getNamespace( )
					: null;

			// resolved status

			if ( target != null )
				return new StructRefValue( namespace, target );

			// unresolved status

			return new StructRefValue( namespace, name );

		}
	}

	/**
	 * Validates the structure value.
	 * 
	 * @param module
	 *            report design
	 * @param defn
	 *            the property definition of the value to validate
	 * @param target
	 *            target structure
	 * @return the resolved structure reference value
	 * @throws PropertyValueException
	 *             if the type of target structure is not that target
	 *             definition.
	 */

	public static StructRefValue resolve( Module module, PropertyDefn defn,
			Structure target ) throws PropertyValueException
	{
		if ( target == null || module == null || defn == null )
			return null;

		assert defn.getTypeCode( ) == PropertyType.STRUCT_REF_TYPE;
		StructureDefn targetDefn = (StructureDefn) defn.getStructDefn( );
		if ( targetDefn != target.getDefn( ) )
			throw new PropertyValueException(
					target.getReferencableProperty( ),
					PropertyValueException.DESIGN_EXCEPTION_WRONG_ITEM_TYPE,
					PropertyType.STRUCT_REF_TYPE );

		// TODO: target need the root namespace now

		return resolve( module, defn, target.getReferencableProperty( ) );
	}
}
