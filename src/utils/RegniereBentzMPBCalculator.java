package utils;

import java.time.LocalDate;


/**The set of equations needed to run the Mountain Pine Beetle winter survival model from:<br><br>
 * 
 *    Régnière, Jacques, and Barbara Bentz.<br>
 *    “Modeling Cold Tolerance in the Mountain Pine Beetle, Dendroctonus Ponderosae.”<br> 
 *    Journal of Insect Physiology 53, no. 6 (June 2007): 559–72. <br>
 *    https://doi.org/10.1016/j.jinsphys.2007.02.007. 
 * 
 * @author michaelfrancenelson */

public class RegniereBentzMPBCalculator {

	/** Constant used in equation 11.  Average between north and south sides of the bole. */
	static final double DELTA_MAX = 3.25;
	
	/** Constant used in equation 11. */
	static final double TEMPERATURE_CONST = 24.4;

	/** Constant from equation 12, used as an adjustment to calculate tau_min */
	static final double TAU_MIN_CONST = 1.8;
	
	/** Mean SCP in State 1  */
	static final double MEAN_SCP_ALPHA_1 = -9.8;
	/** Spread of SCP in State 1  */
	static final double SPREAD_SCP_BETA_1 = 2.26;

	/** Mean SCP in State 2  */
	static final double MEAN_SCP_ALPHA_2 = -21.2;
	/** Spread of SCP in State 2  */
	static final double SPREAD_SCP_BETA_2 = 1.47;

	/** Mean SCP in State 3  */
	static final double MEAN_SCP_ALPHA_3 = -32.3;
	/** Spread of SCP in State 3  */
	static final double SPREAD_SCP_BETA_3 = 2.42;

	/** Maximum gain rate  */
	static final double MAX_GAIN_RATE_RHO_G = 0.311;
	/** Spread of the gain temperature response  */
	static final double SPREAD_GAIN_SIGMA_G = 8.716;

	/** Optimum gain temperature at C = 0  */
	static final double OPTIMAL_GAIN_TEMP_MU_G = -5d;
	/** Optimal gain temperature vs. C  */
	static final double OPTIMAL_GAIN_TEMP_SLOPE_KAPPA_G = -39.3;

	/** Maximum loss rate  */
	static final double MAX_LOSS_RATE_RHO_L = 0.791;
	/** Spread of the loss temperature response  */
	static final double SPREAD_LOSS_SIGMA_L = 3.251;

	/** Optimum loss temperature at C = 0 kL  */
	static final double OPTIMAL_LOSS_TEMP_MU_L = 33.9;
	/** Optimal loss temperature vs C  */
	static final double OPTIMAL_LOSS_SLOPE_KAPPA_L = -32.7;

	/** Threshold C for State 1-2 transition  */
	static final double THRESHOLD_LAMBDA_0 = 0.254;
	/** Threshold C for State 2-3 transition  */
	static final double THRESHOLD_LAMBDA_1 = 0.764;
	
	
	/** Helper, convenience function */
	private static double logistic(double x, double alpha, double beta){
		double term1 = Math.exp(-(x - alpha) / beta);
		return term1 / (beta * Math.pow(1d + term1, 2d));
	}

	/** Convenience wrapper for logistic() */
	public static double sCPDistributionState1(double supercoolingTemperature) { return logistic(supercoolingTemperature, MEAN_SCP_ALPHA_1, SPREAD_SCP_BETA_1); }

	/** Convenience wrapper for logistic() */
	public static double sCPDistributionState2(double supercoolingTemperature) { return logistic(supercoolingTemperature, MEAN_SCP_ALPHA_2, SPREAD_SCP_BETA_2); }

	/** Convenience wrapper for logistic() */
	public static double sCPDistributionState3(double supercoolingTemperature) { return logistic(supercoolingTemperature, MEAN_SCP_ALPHA_3, SPREAD_SCP_BETA_3); }

	/** Equation 2 */
	public static double deltaColdHardeningState(double coldHardeningState, double gain, double loss) { return (1d - coldHardeningState) * gain - coldHardeningState * loss; }

	/** Equation 3 */
	public static double gain(double dailyPhloemTempRangeR,	double dailyPhloemTempMeanTau, double tempOptimumGainTG) { return dailyPhloemTempRangeR * MAX_GAIN_RATE_RHO_G * logistic(dailyPhloemTempMeanTau, tempOptimumGainTG, SPREAD_GAIN_SIGMA_G); }

	/** Equation 4 */
	public static double loss(double dailyPhloemTempRangeR,	double dailyPhloemTempMeanTau, double tempOptimumLossTL) { return dailyPhloemTempRangeR * MAX_LOSS_RATE_RHO_L * logistic(dailyPhloemTempMeanTau, tempOptimumLossTL, SPREAD_LOSS_SIGMA_L);	}

	/** Equation 5 */
	public static double supercoolingTempGain(double coldHardeningState) { return OPTIMAL_GAIN_TEMP_MU_G + OPTIMAL_GAIN_TEMP_SLOPE_KAPPA_G * coldHardeningState; }

	/** Equation 6 */
	public static double supercoolingTempLoss(double coldHardeningState) { return OPTIMAL_LOSS_TEMP_MU_L + OPTIMAL_LOSS_SLOPE_KAPPA_L * coldHardeningState; }

	/** Equation 7 */
	public static double currentColdHardening(double coldHardening,	double gain, double loss, boolean gainOnly){
		double gainTerm = coldHardening + (1d - coldHardening) * gain;
		if(gainOnly == true & coldHardening < 0.5) return gainTerm; 
		else return gainTerm - coldHardening * loss;
	}

	/** Equation 8 */
	public static double medianLethalTemperature(double coldHardeningState)
	{
		double p1 = proportion1(coldHardeningState);
		double p3 = proportion3(coldHardeningState);
		double p2 = proportion2(p1, p3);

		return MEAN_SCP_ALPHA_1 * p1 + MEAN_SCP_ALPHA_2 * p2 + MEAN_SCP_ALPHA_3 * p3;
	}

	/** Equation 9a: Proportion in summer state. 
	 *  This equation is wrong in the paper:
	 *  	The min function has no second term, it should be a 1
	 *  	The denominator for p1 should be 0.5 - lambda0
	 *  	The denominator for p3 should be lambda1 - 0.5  */
	public static double proportion1(double coldHardeningState){
		double term = (0.5 - coldHardeningState) / (0.5 - THRESHOLD_LAMBDA_0);
		return Math.max(0, Math.min(1, term));
	}

	/** Equation 9b: proportion in winter state. 
	 * 	This equation is wrong in the paper:
	 * 		The min function has no second term, it should be a 1
	 *  	The denominator for p1 should be 0.5 - lambda0
	 *  	The denominator for p3 should be lambda1 - 0.5 */
	public static double proportion3(double coldHardeningState){
		double prop3 = (coldHardeningState - 0.5) / (THRESHOLD_LAMBDA_1 - 0.5);
		return Math.max(0, Math.min(1, prop3));
	}

	/** Equation 9c: proportion in fall/spring state. */
	public static double proportion2(double p1, double p3){ return 1d - (p1 + p3); }

	/** Equation 9c: proportion in fall/spring state. */
	public static double proportion2(double coldHardeningState) { return 1d - proportion1(coldHardeningState) - proportion3(coldHardeningState); }

	/** Equation 9 combined */
	public static double[] allProportions(double coldHardeningState)
	{
		double p1 = proportion1(coldHardeningState);
		double p3 = proportion3(coldHardeningState);
		double p2 = proportion2(p1, p3);
		return new double[] {p1, p2, p3};
	}

	/** Equation 10 */
	public static double probSurvival(
			double previousProb,
			double dailyMinimumTemperature,
			double[] proportions)
	{
		double[] alpha = new double[] {MEAN_SCP_ALPHA_1, MEAN_SCP_ALPHA_2, MEAN_SCP_ALPHA_3};
		double[] beta = new double[] {SPREAD_SCP_BETA_1, SPREAD_SCP_BETA_2, SPREAD_SCP_BETA_3};
		
		/* If there is missing data, the minimum temperature will be a super low number.
		 * In that case, don't calculate survival for today, just return yesterday's value */
		if(dailyMinimumTemperature < -300)
			return previousProb;
		else
		{
			if(alpha.length != 3 | beta.length != 3 | proportions.length != 3) throw new IllegalArgumentException("");
			
			double newProb = 0d;

			for(int i = 0; i < 3; i++) newProb += proportions[i] / (1d + Math.exp(-(dailyMinimumTemperature - alpha[i]) / beta[i]));
			return Math.min(previousProb, newProb);
		}
	}

	/** Equation 11: Average phloem maximum temperature (north/south sides) from
	 * Bolstad, P. V., B. J. Bentz, and J. A. Logan. 1997. 
	 * Modelling micro-habitat temperature for Dendroctonus ponderosae 
	 * coleoptera: scolytidae). Ecological Modelling 94:287â€“297. */
	public static double phloemTempTauMax(double tempMin, double tempMax)
	{
		return tempMax + DELTA_MAX * (tempMax - tempMin) / TEMPERATURE_CONST;
	}

	/** Equation 12: Average phloem maximum temperature (north/south sides) from
	 * Bolstad, P. V., B. J. Bentz, and J. A. Logan. 1997. 
	 * Modelling micro-habitat temperature for Dendroctonus ponderosae 
	 * coleoptera: scolytidae). Ecological Modelling 94:287â€“297. */
	public static double phloemTempTauMin(double tempMin) {	return tempMin + TAU_MIN_CONST; }

	/**  Update the beetle survival score given today's min and max temperature. <br> 
	 *  If temperature data for the day is missing, today's update is skipped and 
	 *  the survival remains at yesterday's value. <br><br>
	 * 
	 *  state[0] = cold hardening <br>
	 *  state[1] = gain <br>
	 *  state[2] = loss <br>
	 *  state[3] = survival <br>
	 *  
	 * @param temps today's temperatures [0] = low, [1] = high
	 * @param today today's date1
	 * @param state
	 * @return
	 */
	public static double[] updateState(double[] temps, LocalDate today, double[] state)
	{
		/* state[0] = cold hardening
		 * state[1] = gain
		 * state[2] = loss
		 * state[3] = survival
		 */
		/* If there is missing data for today, skip the update. */
		if(!(temps[0] <= -999 || temps[1] <= -999)){


			boolean gainOnly = false;
			if(today.getMonthValue() >= 8) {gainOnly = true;}

			/* Calculate today's phloem temperatures: */
			double phloemMaxTemp = RegniereBentzMPBCalculator.phloemTempTauMax(temps[0], temps[1]);
			double phloemMinTemp = RegniereBentzMPBCalculator.phloemTempTauMin(temps[0]);

			/* Update the gain and loss: */
			double supercoolingTempGain = RegniereBentzMPBCalculator.supercoolingTempGain(state[0]);
			double supercoolingTempLoss = RegniereBentzMPBCalculator.supercoolingTempLoss(state[0]);

			state[1] = RegniereBentzMPBCalculator.gain(
					phloemMaxTemp - phloemMinTemp, 
					0.5 * (phloemMaxTemp + phloemMinTemp), 
					supercoolingTempGain);
			state[2] = RegniereBentzMPBCalculator.loss(
					phloemMaxTemp - phloemMinTemp, 
					0.5 * (phloemMaxTemp + phloemMinTemp), 
					supercoolingTempLoss);

			state[0] = RegniereBentzMPBCalculator.currentColdHardening( state[0], state[1], state[2], gainOnly);

			state[3] = 	RegniereBentzMPBCalculator.probSurvival(
					state[3], temps[0], RegniereBentzMPBCalculator.allProportions(state[0]));
			return state;
		}
		return state;
	}
	
	
}
