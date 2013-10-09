/*
 * Copyright © 2008, Benecard, All Rights Reserved
 * Unless otherwise indicated, all source code is the copyright of Benecard
 * and is protected by applicable US and international copyright laws,
 * all rights reserved. No part of this material may be used for any purpose
 * without prior written permission. This material and all source codes may
 * not be reproduced in any form without permission in writing from Benecard.
 * Use for any purpose without written permission from Benecard is expressly
 * prohibited by law, and may result in civil and criminal penalties.
 * All rights reserved. No part of this file may be reproduced in any form
 * or by any electronic or mechanical means, including the use of information
 * storage and retrieval systems, without permission in writing from
 * the copyright owner.
 */
package com.benecard.service.delegate.postalAddress;

import java.util.Map;
import java.util.Set;

import com.benecard.commons.IConstants;
import com.benecard.commons.security.Credentials;
import com.benecard.core.service.ServiceResponse;
import com.benecard.pbm.model.City;
import com.benecard.pbm.model.County;
import com.benecard.pbm.model.PostalAddress;
import com.benecard.pbm.model.PostalCode;
import com.benecard.service.delegate.DelegateBase;
import com.benecard.service.ejb.postaladdress.IPostalAddressServiceLocal;

/**
 * PostalAddressDelegate class.
 * 
 * @author edata_rm
 * @date 08/25/2009
 */
public class PostalAddressDelegate extends DelegateBase
{
	@SuppressWarnings("unchecked")
	public Set<City> selectCities( String cityName, Credentials credentials ) throws Exception
	{
		Set<City> cities = null;

		ServiceResponse response = (ServiceResponse) getSessionFacadeRemote().executeService( IConstants.POSTAL_ADDRESS_SERVICE_NAME, IConstants.SELECT_CITIES, new Object[] { cityName }, credentials );

		if ( response.getResponseStatus().isSuccess() )
		{
			cities = (Set<City>) response.getResponseResultObject();
		}
		else
		{
			evaluateError( response );
		}

		return cities;
	}

	public Set<City> selectCities( String cityName ) throws Exception
	{
		return selectCities( cityName, null );
	}

	@SuppressWarnings("unchecked")
	public Set<County> selectCounties( PostalAddress postalAddress, Credentials credentials ) throws Exception
	{
		Set<County> counties = null;

		ServiceResponse response = (ServiceResponse) getSessionFacadeRemote().executeService( IConstants.POSTAL_ADDRESS_SERVICE_NAME, IConstants.SELECT_COUNTIES, new Object[] { postalAddress }, credentials );

		if ( response.getResponseStatus().isSuccess() )
		{
			counties = (Set<County>) response.getResponseResultObject();
		}
		else
		{
			evaluateError( response );
		}

		return counties;
	}

	public Set<County> selectCounties( PostalAddress postalAddress ) throws Exception
	{
		return selectCounties( postalAddress, null );
	}

	@SuppressWarnings("unchecked")
	public Set<PostalCode> selectZipCodes( PostalAddress postalAddress, Credentials credentials ) throws Exception
	{
		Set<PostalCode> postCodes = null;

		ServiceResponse response = (ServiceResponse) getSessionFacadeRemote().executeService( IConstants.POSTAL_ADDRESS_SERVICE_NAME, IConstants.SELECT_ZIPCODES, new Object[] { postalAddress }, credentials );

		if ( response.getResponseStatus().isSuccess() )
		{
			postCodes = (Set<PostalCode>) response.getResponseResultObject();
		}
		else
		{
			evaluateError( response );
		}

		return postCodes;
	}

	public Set<PostalCode> selectZipCodes( PostalAddress postalAddress ) throws Exception
	{
		return selectZipCodes( postalAddress, null );
	}

	@SuppressWarnings("unchecked")
	public Map<String, Boolean> validateAddress( PostalAddress postalAddress, Credentials credentials ) throws Exception
	{
		Map<String, Boolean> result = null;

		ServiceResponse response = (ServiceResponse) getSessionFacadeRemote().executeService( IConstants.POSTAL_ADDRESS_SERVICE_NAME, IConstants.VALIDATE_ADR, new Object[] { postalAddress }, credentials );

		if ( response.getResponseStatus().isSuccess() )
		{
			result = (Map<String, Boolean>) response.getResponseResultObject();
		}
		else
		{
			evaluateError( response );
		}

		return result;
	}

	public Map<String, Boolean> validateAddress( PostalAddress postalAddress ) throws Exception
	{
		return validateAddress( postalAddress, null );
	}

	/**
	 * Method gets State and cities for the zipcode passed.
	 * 
	 * @param zipcode
	 *            String
	 * @param credentials
	 *            Credentials
	 * @return Object[]
	 * @throws Exception
	 */
	public Object[] getStateAndCities( String zipcode, Credentials credentials ) throws Exception
	{
		Object[] result = null;

		ServiceResponse response = (ServiceResponse) getSessionFacadeRemote().executeService( IConstants.POSTAL_ADDRESS_SERVICE_NAME, IPostalAddressServiceLocal.GET_STATE_AND_CITIES, new Object[] { zipcode }, credentials );

		if ( response.getResponseStatus().isSuccess() )
		{
			result = (Object[]) response.getResponseResultObject();
		}
		else
		{
			evaluateError( response );
		}

		return result;
	}
}