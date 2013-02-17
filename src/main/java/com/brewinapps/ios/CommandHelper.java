package com.brewinapps.ios;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.maven.plugin.logging.Log;

/**
 * 
 * @author Brewin' Apps AS
 */
public class CommandHelper {

	/**
	 * @param pb
	 * @throws IOSException
	 */
	public static String performCommand(final ProcessBuilder pb, Log logger) throws IOSException {
		pb.redirectErrorStream(true);
		
		StringBuilder joinedCommand = new StringBuilder();
		for (String segment : pb.command()) {
			joinedCommand.append(segment + " ");
		}
		logger.info("Executing '" + joinedCommand.toString().trim() + "'");
		
		Process p = null;
		try {
			p = pb.start();
		} catch (IOException e) {
			throw new IOSException(e);
		}
		
		BufferedReader input = new BufferedReader(
				new InputStreamReader(p.getInputStream()));
		
		int rc;
		String returnValue = null;
		try {
			// Display output
			String outLine = null;
			StringBuilder sb = new StringBuilder();
			while ((outLine = input.readLine()) != null) {
				sb.append(outLine);
				logger.debug(outLine);
			}
			returnValue = sb.toString();
			input.close();
		} catch (IOException e) {
			throw new IOSException("An error occured while reading the input stream");
		}
		
		try {
			rc = p.waitFor();
		} catch (InterruptedException e) {
			throw new IOSException(e);
		}
		
		if (rc != 0) {
			throw new IOSException("The command was unsuccessful");
		}
		
		return returnValue;
	}
}
