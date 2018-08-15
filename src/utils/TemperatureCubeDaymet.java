package utils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import ucar.nc2.NetcdfFile;

public class TemperatureCubeDaymet implements TemperatureCube{

	private LocalDate startDate;

	private float[][][] temperaturesMin_f;
	private float[][][] temperaturesMax_f;

	public TemperatureCubeDaymet(String min_filename, String max_filename, int year)
	{
		NetcdfFile ncfile_min = null;
		NetcdfFile ncfile_max = null;

		try {
			ncfile_min = NetcdfFile.open(min_filename);
			ncfile_max = NetcdfFile.open(max_filename);
		} catch(IOException ie) { ie.printStackTrace(); }		        

		try {
			temperaturesMin_f = (float[][][]) ncfile_min.findVariable("tmin").read().copyToNDJavaArray();
			temperaturesMax_f = (float[][][]) ncfile_max.findVariable("tmax").read().copyToNDJavaArray();
		} catch (IOException e) { e.printStackTrace(); }

		startDate = LocalDate.of(year, 1, 1);
		
		try {
			ncfile_min.close();
			ncfile_max.close();
		} catch (IOException e) { e.printStackTrace(); }
	}

	@Override
	public double[][] getMaxOnDate(LocalDate date){
		int dayIndex = (int)ChronoUnit.DAYS.between(startDate, date);
		return floatToDouble(temperaturesMax_f[dayIndex]);
	}
	
	@Override
	public double[][] getMinOnDate(LocalDate date){
		int dayIndex = (int)ChronoUnit.DAYS.between(startDate, date);
		return floatToDouble(temperaturesMin_f[dayIndex]);
	}
	

	@Override
	public int getNcols() { return temperaturesMin_f[0].length; }
	
	@Override
	public int getNrows() { return temperaturesMin_f[0][0].length; }

	private double[][] floatToDouble(float[][] in)
	{
		double[][] out = new double[in.length][in[0].length];
		for (int col = 0; col < in.length; col++) for (int row = 0; row < in[0].length; row++)
			out[col][row] = (double) in[col][row];
		return out;
	}
	
}
