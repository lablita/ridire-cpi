/*******************************************************************************
 * Copyright 2013 Università degli Studi di Firenze
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package it.drwolf.ridire.util;

/**
 * Copyright (c) 2010 Mark S. Kolich
 * http://mark.koli.ch
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

public class JumpToLine {

	private File file_;
	private long lastLineRead_;

	private InputStreamReader isr_;
	private LineIterator it_;

	public JumpToLine(File file) throws IOException {
		this.file_ = file;
		this.lastLineRead_ = 1L;
	}

	/**
	 * Closes this IOUtils LineIterator and the underlying input stream reader.
	 */
	public void close() {
		IOUtils.closeQuietly(this.isr_);
		if (this.it_ != null) {
			this.it_.close();
		}
	}

	public File getFile() {
		return this.file_;
	}

	public long getLastLineRead() {
		return this.lastLineRead_;
	}

	/**
	 * Returns true of there are any more lines to read in the file. Otherwise,
	 * returns false.
	 * 
	 * @return
	 */
	public boolean hasNext() {
		return this.it_.hasNext();
	}

	/**
	 * Opens any underlying streams/readers and immeadietly seeks to the line in
	 * the file that's next to be read; skipping over lines in the file this
	 * reader has already read.
	 */
	public void open() throws IOException {
		try {
			this.isr_ = new InputStreamReader(new FileInputStream(this.file_));
			this.it_ = IOUtils.lineIterator(this.isr_);
		} catch (Exception e) {
			this.close();
			throw new IOException(e);
		}
	}

	/**
	 * Read a line of text from this reader.
	 * 
	 * @return
	 */
	public String readLine() {
		String ret = null;
		try {
			// If there is nothing more to read with this LineIterator
			// then nextLine() throws a NoSuchElementException.
			ret = this.it_.nextLine();
			this.lastLineRead_ += 1L;
		} catch (NoSuchElementException e) {
			throw e;
		}
		return ret;
	}

	/**
	 * Seeks to the last line read in the file.
	 */
	public long seek() {
		return this.seek(this.lastLineRead_);
	}

	/**
	 * Seeks to a given line number in a file.
	 * 
	 * @param line
	 */
	public long seek(long line) {
		long lineCount = 1L;
		while (this.it_ != null && this.it_.hasNext() && lineCount < line) {
			this.it_.nextLine();
			lineCount += 1L;
		}
		// If we got to the end of the file, but haven't read as many
		// lines as we should have, then the requested line number is
		// out of range.
		if (lineCount < line) {
			throw new NoSuchElementException("Invalid line number; "
					+ "out of range.");
		}
		this.lastLineRead_ = lineCount;
		return lineCount;
	}

}
