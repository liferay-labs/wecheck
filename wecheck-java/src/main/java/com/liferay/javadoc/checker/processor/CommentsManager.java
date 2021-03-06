/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.javadoc.checker.processor;

import java.io.IOException;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Julio Camarero
 */
@Service
public class CommentsManager {

	public Comment postMessage(Repository repo, int number, String message)
		throws IOException {

		for (int retryCount = 1; retryCount <= _MAX_RETRIES; retryCount++) {
			try {
				return _doPostMessage(repo, number, message);
			}
			catch (IOException ioe) {
				_log.error("Error when posting comment in github.", ioe);

				if (retryCount >= _MAX_RETRIES) {
					throw ioe;
				}

				_log.debug(
					"Retrying in " + _RETRY_PERIOD_DEFAULT + " seconds. (" +
					retryCount + ")");

				sleep(1000 * _RETRY_PERIOD_DEFAULT);
			}
		}

		return null; // Code should never reach this point
	}

	private Comment _doPostMessage(Repository repo, int number, String message)
		throws IOException {

		GitHubClient gitHubClient = new GitHubClient();

		gitHubClient.setCredentials(
			_credentialsManager.getGithubUser(),
			_credentialsManager.getGithubPassword());

		IssueService service = new IssueService(gitHubClient);

		Comment comment = service.createComment(repo, number, message);

		_log.debug(
			"Comment posted successfully to pull request " + number + " - " +
				repo.generateId());

		return comment;
	}

	private void sleep(long duration) {
		try {
			Thread.sleep(duration);
		}
		catch (InterruptedException ie) {
			throw new RuntimeException(ie);
		}
	}

	private static final Logger _log = LoggerFactory.getLogger(
		CredentialsManager.class);

	private final int _MAX_RETRIES = 3;

	private final int _RETRY_PERIOD_DEFAULT = 5;

	@Autowired
	private CredentialsManager _credentialsManager;

}