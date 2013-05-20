package it.drwolf.ridire.utility.test;

import java.util.Date;
import java.util.TimeZone;

public class ProvaDate {
	public static void main(String[] args) {
		Date date = new Date();
		System.out.println(TimeZone.getDefault().getID());
		System.out.println(date);
	}
}
