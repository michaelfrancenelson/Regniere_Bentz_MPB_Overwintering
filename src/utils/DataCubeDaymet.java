package utils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.Variable;

public class DataCubeDaymet implements DataCube {
	
	Dimension lonCoordinateDim;
    Dimension latCoordinateDim;
	Dimension dateCoordinateDim;
    
	Variable lonCoordinateVar;
	Variable latCoordinateVar;
	Variable dateCoordinateVar;

	double[] lonCoordinate, latCoordinate;
	int[] dateCoordinate;
	Array lonCoordinateArray, latCoordinateArray, dateCoordinateArray;
	
	Variable survivalVar;
	
	File templateFile;
//	NetcdfFile templateNCDF;
	
	List<List<Dimension>> otherDimensions;
	List<Variable> otherSpatialVariables;
	List<Variable> otherNonSpatialVariablesDest;
	List<Variable> otherNonSpatialVariablesSource;
	List<Array> otherSpatialArrays;
	List<Attribute> globalAttributes;

	Array data;
	Index index;
	
	NetcdfFileWriter dataFile;
	
	public void addDataLayer(int layer, double[][] dat){
		index = data.getIndex();
//		for(int col = 0; col < dat[0].length; col++) for(int row = 0; row < dat.length; row++){
		for(int col = 0; col < dat.length; col++) for(int row = 0; row < dat[0].length; row++){
			index.set(layer, row, col);
//			data.setDouble(index, dat[row][col]);
			data.setDouble(index, dat[col][row]);
		}
	}

	public int[][] initialize(String filenameTemplateDest, String filenameOutput, int startYear, int endYear)
	{
		
		/* Use the first year's min temp file as a template */
//		String filenameTemplateDest = inputDataDirectory  + tileID + "_" + startYear + "_tmin.nc";
		
		/* The cube of survival values will be saved here: */
//		String filenameOutput = outputDataDirectory + tileID + "_" + startYear + "_" + endYear + "_modeled_beetle_survival.nc";
		
		/* how many years? */
		int nYears = endYear - startYear;

		
		
		Array latArray, lonArray = null;
		Variable latVar, lonVar, conicVar, conic;
		Dimension latDim, lonDim;

		otherSpatialArrays = new ArrayList<Array>();
		otherDimensions = new ArrayList<List<Dimension>>();
		otherSpatialVariables = new ArrayList<Variable>();
		otherNonSpatialVariablesDest = new ArrayList<Variable>();
		otherNonSpatialVariablesSource = new ArrayList<Variable>();
		
		Array templateDataArray = null;

		try {

			NetcdfFile templateNCDF = NetcdfFile.open(filenameTemplateDest);
			dataFile = NetcdfFileWriter.createNew(Version.netcdf3, filenameOutput);

			/* Make copies of the projection info, and the lat/lon coordinates. */
			conic = new Variable(templateNCDF.findVariable("lambert_conformal_conic")); 
			latArray = templateNCDF.findVariable("lat").read().copy();
			lonArray = templateNCDF.findVariable("lon").read().copy();
			
			/* For building the Dimensions below.  There may be a way to do this without these intermediate arrays... */
			latCoordinateArray = Array.factory(templateNCDF.findVariable("y").read().copyToNDJavaArray());
			lonCoordinateArray = Array.factory(templateNCDF.findVariable("x").read().copyToNDJavaArray());
			
			templateDataArray = templateNCDF.findVariable("tmin").read().copy();
			
			otherNonSpatialVariablesSource.add(conic);

			/* Create the year coordinate dimensions. */
			dateCoordinateDim = dataFile.addDimension(null, "winter_ending_in", nYears + 1);
			List<Dimension> dateDims = new ArrayList<Dimension>(); dateDims.add(dateCoordinateDim);
			dateCoordinateVar = dataFile.addVariable(null, "winter_ending_in", DataType.INT, dateDims);
			dateCoordinate = new int[nYears + 1];
			for(int i = 0; i < nYears + 1; i++)
				dateCoordinate[i] =  startYear + i + 1;
			dateCoordinateArray = Array.factory(dateCoordinate);

			/* Build the lon/lat coordinates: */
			latDim = dataFile.addDimension(null, "y", latCoordinateArray.getShape()[0]);
			lonDim = dataFile.addDimension(null, "x", lonCoordinateArray.getShape()[0]);
			latCoordinateVar = dataFile.addVariable(null, "y", DataType.DOUBLE, "y");
			lonCoordinateVar = dataFile.addVariable(null, "x", DataType.DOUBLE, "x");
			List<Dimension> lonLatDims = new ArrayList<Dimension>();
			lonLatDims.add(latDim); 
			lonLatDims.add(lonDim);
			lonVar = dataFile.addVariable(null, "lon", DataType.DOUBLE, lonLatDims);
			latVar = dataFile.addVariable(null, "lat", DataType.DOUBLE, lonLatDims);
			otherSpatialVariables.add(latVar); otherSpatialVariables.add(lonVar);
			otherSpatialArrays.add(latArray); otherSpatialArrays.add(lonArray);

			/* Build the survival output array. */
			List<Dimension> survivalDims = new ArrayList<Dimension>(); survivalDims.add(dateCoordinateDim);
			survivalDims.add(latDim);
			survivalDims.add(lonDim);
			survivalVar = dataFile.addVariable(null, "predicted_survival", DataType.DOUBLE, survivalDims);
			
			int nRows = latArray.getShape()[1];
			int nCols = latArray.getShape()[0];
			
			data = Array.factory(DataType.DOUBLE, new int[]{nYears + 1, nCols, nRows});
  			
			/* Projection info: */
			conicVar = dataFile.addVariable(null, "lambert_conformal_conic", DataType.SHORT, conic.getDimensionsAll());
			otherNonSpatialVariablesDest.add(conicVar);

			/* Make sure the attributes make it to the data file: */
			latCoordinateVar.addAll(templateNCDF.findVariable("y").getAttributes());
			lonCoordinateVar.addAll(templateNCDF.findVariable("x").getAttributes());
			latVar.addAll(templateNCDF.findVariable("lat").getAttributes());
			lonVar.addAll(templateNCDF.findVariable("lon").getAttributes());
			conicVar.addAll(templateNCDF.findVariable("lambert_conformal_conic").getAttributes());

			globalAttributes = templateNCDF.getGlobalAttributes();

			for (Attribute a : globalAttributes) dataFile.addGroupAttribute(null, a);
		} catch (IOException e) {e.printStackTrace();}
		
		/* Mark the cells not to process: */
		Index lonIndex = lonArray.getIndex();
		Index datIndex = data.getIndex();
		Index tempIndex = templateDataArray.getIndex();
		int[][] maskArray = new int[lonIndex.getShape(0)][lonIndex.getShape(1)];
		for(int year = 0; year < nYears; year++)
		for(int col = 0; col < maskArray.length; col++) for(int row = 0; row < maskArray[0].length; row++)
		{
			tempIndex.set(0, col, row);
			if(templateDataArray.getDouble(tempIndex) < -900)
			{
				datIndex.set(year, col, row);
				data.setDouble(datIndex, -1);
				maskArray[col][row] = -1;
			}
		}
		return maskArray;
	}
	
	public void initializeSaveFile(String filename, int startYear, int endYear, int nCol, int nRow, TemperatureCube weather) throws IOException{
		int nYears = endYear - startYear + 1;
		dataFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, filename);
		
		if(nRow < 0){nCol = weather.getNcols(); nRow = weather.getNrows();}
		
		/* Create coordinate dimensions */
		lonCoordinateDim = dataFile.addDimension(null, "lon", nCol);
	    latCoordinateDim = dataFile.addDimension(null, "lat", nRow);
    	dateCoordinateDim = dataFile.addDimension(null, "year", nYears);
    	
    	lonCoordinateVar = dataFile.addVariable(null,"lon", DataType.DOUBLE, "lon");
    	latCoordinateVar = dataFile.addVariable(null,"lat", DataType.DOUBLE, "lat");
    	dateCoordinateVar = dataFile.addVariable(null,"year", DataType.INT, "year");
    	
    	List<Dimension> dims = new ArrayList<Dimension>();
    	dims.add(dateCoordinateDim);
    	dims.add(lonCoordinateDim);
    	dims.add(latCoordinateDim);
    	
    	survivalVar = dataFile.addVariable(null, "percent_survival", DataType.DOUBLE, dims);
    	data = Array.factory(DataType.DOUBLE, new int[]{nYears, nCol, nRow});
	}
	
	public void writeOutputFile()
	{
		try {
			dataFile.create();
			dataFile.write(latCoordinateVar, latCoordinateArray);
			dataFile.write(lonCoordinateVar, lonCoordinateArray);
			for(int i = 0; i < otherSpatialVariables.size(); i++)
				dataFile.write(otherSpatialVariables.get(i), otherSpatialArrays.get(i));
			for(int i = 0; i < otherNonSpatialVariablesDest.size(); i++)
				dataFile.write(otherNonSpatialVariablesDest.get(i), otherNonSpatialVariablesSource.get(i).read());
			dataFile.write(dateCoordinateVar, Array.factory(dateCoordinate));
			dataFile.write(survivalVar, data);
			dataFile.close();
//			templateNCDF.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidRangeException e) {
			e.printStackTrace();
		}
	}
	
//	/** Default, save using the settings from the temperature cube. */
//	public void finalizeSaveFile(){
//		try {
//			finalizeSaveFile(startYear, nYears + startYear);
//		} catch (IOException e) {e.printStackTrace();
//		} catch (InvalidRangeException e) {e.printStackTrace();}
//	}

	public void finalizeSaveFile(int startYear, int endYear) throws IOException, InvalidRangeException{
		LocalDate date = LocalDate.of(startYear - 1, 1, 1);
		int[] years   = new int[endYear - startYear + 1];
    	for(int i = 0; i < years.length; i++){
    		date = date.plusYears(1);
			years[i] = date.getYear();
		}
		dataFile.create();
		dataFile.write(lonCoordinateVar, lonCoordinateArray);
		dataFile.write(latCoordinateVar, latCoordinateArray);
		dataFile.write(dateCoordinateVar, Array.factory(years));
		dataFile.write(survivalVar, data);
		dataFile.close();
	}
}