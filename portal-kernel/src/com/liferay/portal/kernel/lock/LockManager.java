/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.kernel.lock;

import aQute.bnd.annotation.ProviderType;

import com.liferay.portal.kernel.exception.PortalException;

import java.util.Optional;

/**
 * @author Tina Tian
 */
@ProviderType
public interface LockManager {

	public void clear();

	public Lock createLock(
		long lockId, long companyId, long userId, String userName);

	public Lock getLock(String className, long key) throws PortalException;

	public Lock getLock(String className, String key) throws PortalException;

	public Lock getLockByUuidAndCompanyId(String uuid, long companyId)
		throws PortalException;

	public boolean hasLock(long userId, String className, long key);

	public boolean hasLock(long userId, String className, String key);

	public boolean isLocked(String className, long key);

	public boolean isLocked(String className, String key);

	/**
	 * @deprecated As of 7.0.0, see {@link #tryLock(
	 * 				long, String, long, String, boolean, long)}
	 */
	@Deprecated
	public Lock lock(
			long userId, String className, long key, String owner,
			boolean inheritable, long expirationTime)
		throws PortalException;

	/**
	 * @deprecated As of 7.0.0, see {@link #tryLock(
	 * 				long, String, String, String, boolean, long)}
	 */
	@Deprecated
	public Lock lock(
			long userId, String className, String key, String owner,
			boolean inheritable, long expirationTime)
		throws PortalException;

	/**
	 * @deprecated As of 7.0.0, see {@link #tryLock(String, String, String)}
	 */
	@Deprecated
	public Lock lock(String className, String key, String owner);

	/**
	 * @deprecated As of 7.0.0, see {@link #tryLock(
	 * 				String, String, String, String)}
	 */
	@Deprecated
	public Lock lock(
		String className, String key, String expectedOwner,
		String updatedOwner);

	public Lock refresh(String uuid, long companyId, long expirationTime)
		throws PortalException;

	public Optional<Lock> tryLock(
		long userId, String className, long key, String owner,
		boolean inheritable, long expirationTime);

	public Optional<Lock> tryLock(
		long userId, String className, String key, String owner,
		boolean inheritable, long expirationTime);

	public Optional<Lock> tryLock(String className, String key, String owner);

	public Optional<Lock> tryLock(
		String className, String key, String expectedOwner,
		String updatedOwner);

	public void unlock(String className, long key);

	public void unlock(String className, String key);

	public void unlock(String className, String key, String owner);

}