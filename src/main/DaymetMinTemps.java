package main;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.Variable;
import utils.DataCubeDaymet;
import utils.TemperatureCubeDaymet;

public class DaymetMinTemps {

	DataCubeDaymet data_cube;
	TemperatureCubeDaymet temp_cube;

	int winter_start_day = 240;
	int winter_end_day = 120;

	int tile_number;
	int start_year;
	int end_year;

	int[][] mask;

	String tmin_suffix = "_tmin.nc";
	String tmax_suffix = "_tmax.nc";


	public double[][] getAnnualMin(String min_filename, String max_filename, int year)
	{
		LocalDate start_date = LocalDate.of(year, 1, 1);
		LocalDate next_date = start_date.plusDays(1);

		temp_cube = new TemperatureCubeDaymet(min_filename, max_filename, year);
		double[][] current_min = temp_cube.getMinOnDate(start_date);

		System.out.println("year: " + start_date.getYear());
		while (next_date.getYear() == year)
		{
			//			System.out.println("year = " + next_date.getYear() + ", day = " + next_date.getDayOfYear());
			cellwiseMin(current_min, temp_cube.getMinOnDate(next_date));
			if (next_date.isLeapYear() & next_date.getDayOfYear() == 365) next_date = next_date.plusYears(1);
			next_date = next_date.plusDays(1);
		}
		return applyMask(current_min);
	}

	public double[][] getWinterMin(String min_filename, String min_filename_2, String max_filename, String max_filename_2, int year)
	{
		LocalDate start_date = LocalDate.of(year, 9, 1); 
		LocalDate next_date = start_date.plusDays(1);

		temp_cube = new TemperatureCubeDaymet(min_filename, max_filename, year);
		TemperatureCubeDaymet temp_cube_2 = new TemperatureCubeDaymet(min_filename_2, max_filename_2, year + 1);;

		double[][] current_min = temp_cube.getMinOnDate(start_date);

		System.out.println("winter start year: " + start_date.getYear());
		while (next_date.getYear() == year)
		{
			cellwiseMin(current_min, temp_cube.getMinOnDate(next_date));
			if (next_date.isLeapYear() & next_date.getDayOfYear() == 365) next_date = next_date.plusYears(1);
			next_date = next_date.plusDays(1);
		}
		System.out.println("winter end   year: " + next_date.getYear());
		while (next_date.getDayOfYear() < winter_end_day)
		{
			cellwiseMin(current_min, temp_cube_2.getMinOnDate(next_date));
			if (next_date.isLeapYear() & next_date.getDayOfYear() == 365) next_date = next_date.plusYears(1);
			next_date = next_date.plusDays(1);
		}
		return applyMask(current_min);
	}


	private double[][] applyMask(double[][] input)
	{
		for (int i = 0; i < input.length; i++) for (int j = 0; j < input[0].length; j++)
		{
			if (mask[i][j] == -1)
				input[i][j] = Double.NaN;
		}
		return input;
	}



	/** Does not check for array size equality. */
	private void cellwiseMin(double[][] current, double[][] next)
	{
		for (int i = 0; i < current.length; i++) for (int j = 0; j < current[0].length; j++)
		{
			if (mask[i][j] > -1)
				current[i][j] = Math.min(current[i][j], next[i][j]);
		}
	}

	public void runWinterMin(int tileID, int startYear, int endYear, String inputDataDirectory, String outputDirectory)
	{

		String input_file_name_base = inputDataDirectory + tileID + "_" ;

		String tmin_file_name = input_file_name_base + startYear + tmin_suffix;
		String tmax_file_name = input_file_name_base + startYear + tmax_suffix;

		String tmin_file_name_2 = input_file_name_base + (startYear + 1) + tmin_suffix;
		String tmax_file_name_2 = input_file_name_base + (startYear + 1) + tmax_suffix;

		String filenameOutput = outputDirectory + tileID + "_" + startYear + "_" + endYear + "_winter_tmin.nc";		

		data_cube = new DataCubeDaymet();
		mask = data_cube.initialize(tmin_file_name, filenameOutput, startYear, endYear, "winter_tmin", "winter_ending_in", 1);

		int nYears = endYear - startYear + 1;

		for (int i = 0; i < nYears - 1; i++)
		{
			tmin_file_name = input_file_name_base + (startYear + i) + tmin_suffix;
			tmax_file_name = input_file_name_base + (startYear + i) + tmax_suffix;
			tmin_file_name_2 = input_file_name_base + (startYear + i + 1) + tmin_suffix;
			tmax_file_name_2 = input_file_name_base + (startYear + i + 1) + tmax_suffix;

			double[][] data = getWinterMin(tmin_file_name, tmin_file_name_2, tmax_file_name, tmax_file_name_2, startYear + i);
			data_cube.addDataLayer(i, data);
		}

		data_cube.writeOutputFile();

	}

	public void runAnnualMin(int tileID, int startYear, int endYear, String inputDataDirectory, String outputDirectory)
	{
		String input_file_name_base = inputDataDirectory + tileID + "_" ;

		String tmin_file_name = input_file_name_base + startYear + tmin_suffix;
		String tmax_file_name = input_file_name_base + startYear + tmax_suffix;

		String filenameOutput = outputDirectory + tileID + "_" + startYear + "_" + endYear + "_annual_tmin.nc";		

		data_cube = new DataCubeDaymet();
		mask = data_cube.initialize(tmin_file_name, filenameOutput, startYear, endYear, "annual_tmin", "year", 0);

		int nYears = endYear - startYear + 1;

		for (int i = 0; i < nYears; i++)
		{
			tmin_file_name = input_file_name_base + (startYear + i) + tmin_suffix;
			tmax_file_name = input_file_name_base + (startYear + i) + tmax_suffix;
			double[][] data = getAnnualMin(tmin_file_name, tmax_file_name, startYear + i);
			data_cube.addDataLayer(i, data);
		}

		data_cube.writeOutputFile();

	}

	public static List<String> findFoldersInDirectory(String directoryPath) {
		File directory = new File(directoryPath);

		FileFilter directoryFileFilter = new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory();
			}
		};

		File[] directoryListAsFile = directory.listFiles(directoryFileFilter);
		List<String> foldersInDirectory = new ArrayList<String>(directoryListAsFile.length);
		for (File directoryAsFile : directoryListAsFile) {
			foldersInDirectory.add(directoryAsFile.getName());
		}

		return foldersInDirectory;
	}

	public static void main (String[] args)
	{
		int tileID = 10832;
		int startYear = 1980;
		int endYear = 2016;


//		String[] tiles = new String[] {"10842", "11739", "12090", "12091", "12268", "12269", "12269", "12449", "12628", "12629", "12632", "12806", "12809", "12814"};
//		String[] tiles = new String[] {"12268", "12269", "12449", "12628", "12629", "12632", "12806", "12809", "12814"};
		String[] tiles = new String[] {"12452", "12453"};
		//		String[] tiles = new String[] {"12090", "12091", "12268", "12269", "12269", "12449", "12628", "12629", "12632", "12806", "12809", "12814"};

		String inputDataDirectory = "E:/Data/Daymet/Tiles/" + tileID + "/";
		String outputDirectory = "E:/Data/Daymet/annual_tmin_java/";
		String outputDirectory_wmin = "E:/Data/Daymet/winter_tmin_java/";

		List<String> tileIDs = findFoldersInDirectory("E:/Data/Daymet/Tiles/");

		DaymetMinTemps dm = new DaymetMinTemps();

		//		String tile = "11729";
		//		tileID = Integer.parseInt(tile);
		//		System.out.println("tile id: " + tile);

		inputDataDirectory = "E:/Data/Daymet/Tiles/" + tileID + "/";
		//		dm.runAnnualMin(tileID, startYear, endYear, inputDataDirectory, outputDirectory);
		//		dm.runWinterMin(tileID, startYear, endYear, inputDataDirectory, outputDirectory_wmin);



		//	for (int i = 103; i < tileIDs.size(); i++)
		for (String tile : tiles)
		{
			System.out.println("tile id: " + tile);
			tileID = Integer.parseInt(tile);
			inputDataDirectory = "E:/Data/Daymet/Tiles/" + tileID + "/";
			dm.runAnnualMin(tileID, startYear, endYear, inputDataDirectory, outputDirectory);
			dm.runWinterMin(tileID, startYear, endYear, inputDataDirectory, outputDirectory_wmin);
		}

		//		for (String tile : tileIDs)
		//		for (int i = 103; i < tileIDs.size(); i++)
		////		for (int i = 75; i < 103; i++)
		//		{
		//			String tile = tileIDs.get(i);
		//			System.out.println("tile id: " + tile);
		//			tileID = Integer.parseInt(tile);
		//			inputDataDirectory = "E:/Data/Daymet/Tiles/" + tileID + "/";
		//			dm.runAnnualMin(tileID, startYear, endYear, inputDataDirectory, outputDirectory);
		////			dm.runWinterMin(tileID, startYear, endYear, inputDataDirectory, outputDirectory_wmin);
		//		}
	}



}
