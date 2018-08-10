package utils;

import java.io.IOException;

import ucar.ma2.InvalidRangeException;

public interface DataCube {
	
	public int[][] initialize(String filenameTemplateDest, String filenameOutput, int startYear, int endYear);
	public void addDataLayer(int layer, double[][] dat);
	public void writeOutputFile();
	public void initializeSaveFile(String filename, int startYear, int endYear, int nCol, int nRow, TemperatureCube weather) throws IOException;
	public void finalizeSaveFile(int startYear, int endYear) throws IOException, InvalidRangeException;
}
