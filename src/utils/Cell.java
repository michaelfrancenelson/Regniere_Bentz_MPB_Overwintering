package utils;

import java.time.LocalDate;
 /** @author michaelfrancenelson */

public class Cell {
	/** state[0] = cold hardening <br>
	 *  state[1] = gain <br>
	 *  state[2] = loss <br>
	 *  state[3] = survival */
	public double[] state = new double[4];
	
	public Cell() { state[3] = 1.0f; }
	
	public void reset() { state =  new double[4]; state[3] = 1.0f; }
	
	/** Update the beetle survival score given today's min and max temperature. <br> 
	 *  If temperature data for the day is missing, today's update is skipped and 
	 *  the survival remains at yesterday's value.
	 * 
	 * @param temps today's temperature data: index 0 is the daily min, index 1 is the daily max
	 * @param today a LocalDate object with today's date
	 * @param params */
	public void updateDaily(double[] temps, LocalDate today) { RegniereBentzMPBCalculator.updateState(temps, today, state);
	}
}
