package utils;

import java.time.LocalDate;

public interface TemperatureCube {

	/** Get the layer of maximum temperatures for a date
	 * 
	 * @param date Query date
	 * @return 2D array of temperatures */
	public double[][] getMaxOnDate(LocalDate date);
	/** Get the layer of minimum temperatures for a date
	 * 
	 * @param date Query date
	 * @return 2D array of temperatures */
	public double[][] getMinOnDate(LocalDate date);
	
	public int getNrows();
	public int getNcols();
}
