/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core.connection;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.update.internal.core.IStatusCodes;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.core.UpdateCore;

public class HttpResponse extends AbstractResponse {
	/**
	 * Monitored InputStream.  Upon IOException, discards
	 * connection so it is not resused.
	 *
	 */
	private class MonitoringInputStream extends FilterInputStream {
		InputStream in;

		public MonitoringInputStream(InputStream in) {
			super(in);
			this.in = in;
		}

		public int available() throws IOException {
			try {
				return super.available();
			} catch (IOException ioe) {
				connection = null;
				throw ioe;
			}
		}

		public void close() throws IOException {
			try {
				super.close();
			} catch (IOException ioe) {
				connection = null;
				throw ioe;
			}
		}

		public int read() throws IOException {
			try {
				return super.read();
			} catch (IOException ioe) {
				connection = null;
				throw ioe;
			}
		}

		public synchronized void reset() throws IOException {
			try {
				super.reset();
			} catch (IOException ioe) {
				connection = null;
				throw ioe;
			}
		}

		public int read(byte[] b) throws IOException {
			try {
				return super.read(b);
			} catch (IOException ioe) {
				connection = null;
				throw ioe;
			}
		}

		public int read(byte[] b, int off, int len) throws IOException {
			try {
				return super.read(b, off, len);
			} catch (IOException ioe) {
				connection = null;
				throw ioe;
			}
		}

		public long skip(long n) throws IOException {
			try {
				return super.skip(n);
			} catch (IOException ioe) {
				connection = null;
				throw ioe;
			}
		}

	}
	
	protected URL url;
	protected InputStream in;
	protected long lastModified;
	protected long offset;

	protected HttpResponse(URL url) {
		System.out.println(url);
        this.url = url;
	}

	public InputStream getInputStream() throws IOException {
		if (in == null && url != null) {
			if (connection == null || offset > 0)
				connection = url.openConnection();
			if (offset > 0)
				connection.setRequestProperty("Range", "bytes=" + offset + "-"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			try {
				in = new MonitoringInputStream(connection.getInputStream());
			} catch (IOException ioe) {
				connection = null;
				throw ioe;
			}
			checkOffset();
		}
		return in;
	}
	/**
	 * @see IResponse#getInputStream(IProgressMonitor)
	 */
	public InputStream getInputStream(IProgressMonitor monitor)
		throws IOException, CoreException, TooManyOpenConnectionsException {
		if (in == null && url != null) {
			if (connection == null || offset > 0)
				connection = url.openConnection();
			if (offset > 0)
				connection.setRequestProperty("Range", "bytes=" + offset + "-"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			if (monitor != null) {
				try {
					this.in = new MonitoringInputStream(openStreamWithCancel(
							connection, monitor));
				} catch (IOException ioe) {
					connection = null;
					throw ioe;
				}
			} else {
				try {
					this.in = new MonitoringInputStream(connection
							.getInputStream());
				} catch (IOException ioe) {
					connection = null;
					throw ioe;
				}
			}
			// this can also be run inside a monitoring thread, but it is safe
			// to
			// just call it now, if the input stream has already been obtained
			checkOffset();
			if (in != null) {
				this.lastModified = connection.getLastModified();
			}
		}
		return in;
	}

	public long getContentLength() {
		if (connection != null)
			return connection.getContentLength();
		return 0;
	}

	public int getStatusCode() {
		if (connection == null)
			try {
				connection = url.openConnection();
			} catch (IOException e) {
			}
		if (connection != null) {
			try {
				return ((HttpURLConnection) connection).getResponseCode();
			} catch (IOException e) {
				UpdateCore.warn("", e); //$NON-NLS-1$
			}
		}
		return IStatusCodes.HTTP_OK;
	}

	public String getStatusMessage() {
		if (connection != null) {
			try {
				return ((HttpURLConnection) connection).getResponseMessage();
			} catch (IOException e) {
				UpdateCore.warn("", e); //$NON-NLS-1$
			}
		}
		return ""; //$NON-NLS-1$
	}

	public long getLastModified() {
		if (lastModified == 0) {
			if (connection == null)
				try {
					connection = url.openConnection();
				} catch (IOException e) {
				}
			if (connection != null)
				lastModified = connection.getLastModified();
		}
		return lastModified;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}
	private void checkOffset() throws IOException {
		if (offset == 0)
			return;
		String range = connection.getHeaderField("Content-Range"); //$NON-NLS-1$
		//System.out.println("Content-Range=" + range);
		if (range == null) {
			//System.err.println("Server does not support ranges");
			throw new IOException(Messages.HttpResponse_rangeExpected); 
		} else if (!range.startsWith("bytes " + offset + "-")) { //$NON-NLS-1$ //$NON-NLS-2$
			//System.err.println("Server returned wrong range");
			throw new IOException(Messages.HttpResponse_wrongRange); 
		}
	}
}
