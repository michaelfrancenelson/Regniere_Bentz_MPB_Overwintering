package main;

import utils.DataCubeDaymet;
import utils.TemperatureCubeDaymet;

public class DaymetRunner extends ModelRunner{

	public static void main(String[] args)
	{
		int tileID = -1;
		int startYear = -1;
		int endYear = -1;
		String inputDataDirectory = null;
		String outputDirectory = null;
		
		if (args.length > 0)
		{
			tileID = Integer.parseInt(args[0]);
			startYear = Integer.parseInt(args[1]);
			endYear = Integer.parseInt(args[2]);
			inputDataDirectory = args[3];
			outputDirectory = args[4];
		}
		/* java -jar DaymetRunner.jar 10832 1980 1985 E:/Data/Daymet/Tiles/10832/ E:/Data/Daymet/Tiles/10832/ */ 
		/* Uncomment and edit these to specify arguments within this source. */
//		int tileID = 10832;
//		int startYear = 1980;
//		int endYear = 1985;
//		String inputDataDirectory = "E:/Data/Daymet/Tiles/" + tileID + "/";
//		String outputDirectory = inputDataDirectory;
		
		DaymetRunner runner = new DaymetRunner();
		runner.run(tileID, startYear, endYear, inputDataDirectory, outputDirectory);
	}
	
	
	public void run(int tileID, int startYear, int endYear, String inputDataDirectory, String outputDirectory)
	{
	
		String input_file_name_base = inputDataDirectory + tileID + "_" ;
		String tmin_suffix = "_tmin.nc";
		String tmax_suffix = "_tmax.nc";
		
		String tmin_file_name = input_file_name_base + startYear + tmin_suffix;
		String tmax_file_name = input_file_name_base + startYear + tmax_suffix;
		
		String filenameOutput = outputDirectory + tileID + "_" + startYear + "_" + endYear + "_modeled_beetle_survival.nc";		
		data_cube = new DataCubeDaymet();
		
		temperatures = new TemperatureCubeDaymet(tmin_file_name, tmax_file_name, startYear);

		int nRow, nCol, nYears;

		mask = data_cube.initialize(tmin_file_name, filenameOutput, startYear, endYear);

		nRow = temperatures.getNrows(); nCol = temperatures.getNcols();
		nYears = endYear - startYear + 1;

		createCells(nCol, nRow);
		
		/* Daymet temperatures are in degrees C, no adjustment needed. */
		double tempAdjust = 0;
		
		/* Run the simulation once for the start year in order to prime the cells' states: */
		initializeDay = 180;
		scoreDay = 150;
		stepYear(startYear, tempAdjust, initializeDay, scoreDay);
		
		/* Henceforth, initialization happens on January first. */
		initializeDay = 1;

		for(int i = 0; i < nYears; i++)
		{
			
			System.out.println("TileID: " + tileID + " year: " + (startYear + i));
			tmin_file_name = input_file_name_base + (startYear + i) + tmin_suffix;
			tmax_file_name = input_file_name_base + (startYear + i) + tmax_suffix;
			temperatures = new TemperatureCubeDaymet(tmin_file_name, tmax_file_name, startYear + i);
			double[][] data = stepYear(startYear + i, tempAdjust, initializeDay, scoreDay); 
			data_cube.addDataLayer(i, data);
		}
		data_cube.writeOutputFile();
	}
}
