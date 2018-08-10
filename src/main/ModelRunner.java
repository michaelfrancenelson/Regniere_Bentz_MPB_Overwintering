package main;

import java.time.LocalDate;

import utils.Cell;
import utils.DataCube;
import utils.TemperatureCube;

public class ModelRunner {

	Cell[][] cells;
	int[][] mask;
	TemperatureCube temperatures;
	
	DataCube data_cube;
	
	int initializeDay, scoreDay;
	
	
	/**
	 * 
	 * @param year
	 * @param weather
	 * @param cells
	 * @param tempAdjust
	 * @param startDay
	 * @param scoreDay
	 * @return
	 */
	public double[][] stepYear(
			int year, 
			double tempAdjust, 
			int startDay,
			int scoreDay
			)
	{
		LocalDate today = LocalDate.ofYearDay(year, startDay);
		
		/* End date is December 31. */
		LocalDate endDate = LocalDate.of(year, 1, 1).plusYears(1).minusDays(1);

		int n_col = temperatures.getNcols();
		int n_row = temperatures.getNrows();

		double[][] min_daily_layer;
		double[][] max_daily_layer;

		/* TODO Why are columns first? */
		double[][] data = new double[n_col][n_row];

		while(today.isBefore(endDate)){

			min_daily_layer = temperatures.getMinOnDate(today);
			max_daily_layer = temperatures.getMaxOnDate(today);

			/* Update the cells' temperatures to today's value */
			for(int col = 0; col < n_col; col++) for(int row = 0; row < n_row; row++)
				if (cells[col][row] != null)
				{
//					double[] temps = new double[]{(double)min_daily_layer[col][row] - tempAdjust, (double)max_daily_layer[col][row] - tempAdjust};
					double[] temps = new double[]{(double)min_daily_layer[row][col] - tempAdjust, (double)max_daily_layer[row][col] - tempAdjust};
					cells[col][row].updateDaily(temps, today);
				}

			today = today.plusDays(1);

			/* Gather the survival scores if it is score day.*/
			if(today.getDayOfYear() == scoreDay)
			{
				for(int col = 0; col < n_col; col++)	for(int row = 0; row < n_row; row++)
					if (cells[col][row] != null)
					{
						data[col][row] = cells[col][row].state[3];
						cells[col][row].reset();
					}
					else data[col][row] = Double.NaN;
			}
		}
		return data;
	}
	
	public void createCells(int nCol, int nRow)
	{
		cells = new Cell[nCol][nRow];
		mask = new int[nCol][nRow];
		for(int i = 0; i < nRow; i++) for(int j = 0; j < nCol; j++)
			{
			if (mask[j][i] == 0) 
				cells[j][i] = new Cell();
			else 
				cells[j][i] = null;
			}
	}


}
