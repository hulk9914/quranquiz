/****
* Copyright (C) 2011-2013 Quran Quiz Net 
* Tarek Eldeeb <tarekeldeeb@gmail.com>
* License: see LICENSE.txt
****/
package net.quranquiz.storage;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Vector;

import net.quranquiz.model.QQScoreRecord;
import net.quranquiz.model.QQSparseResult;
import net.quranquiz.model.QQStudyPart;
import net.quranquiz.util.QQUtils;

import com.google.analytics.tracking.android.Log;

public class QQProfile implements Serializable {

	private static final long serialVersionUID = 21L;
	private String uid;
	private int lastSeed; // Seed for the Question
	private int level;
	private Vector<QQStudyPart> QParts;
	private Vector<QQScoreRecord> QScores;
	private boolean specialEnabled;
	private int specialScore;

	public QQProfile(String uid, int lastSeed, int level, String QPartsString,
			String QScoresString, int specialScore) {

		setLastSeed(lastSeed);
		setLevel(level);
		setStudyParts(QPartsString);
		setScoreHistory(QScoresString);
		setuid(uid);
		specialEnabled = true;
		setSpecialScore(specialScore);
	}

	public void addCorrect(int currentPart) {
		if (currentPart < QParts.size()) {
			QParts.get(currentPart).addCorrect(level);
		}
	}

	public void addIncorrect(int currentPart) {
		if (currentPart < QParts.size()) {
			QParts.get(currentPart).addIncorrect();
		}
	}

	public double getAvgLevel(int part) {
		if (part < QParts.size()) {
			return QParts.get(part).getAvgLevel();
		} else {
			return 1.0;
		}	
	}

	public int getCorrect(int part) {
		if (part < QParts.size()) {
			return QParts.get(part).getNumCorrect();
		} else {
			return 0;
		}
	}

	public int getLastSeed() {
		return lastSeed;
	}

	public int getLevel() {
		return level;
	}

	public int getQuesCount(int part) {
		if (part < QParts.size()) {
			return QParts.get(part).getNumQuestions();
		} else {
			return 0;
		}
	}

	public boolean isSpecialEnabled(){
		return specialEnabled;
	}
	
	public void setSpecialEnabled(boolean b){
		specialEnabled = b;
	}
	
	public int getScore() {
		
		double score=0.0;
		for(int i=0;i<QParts.size();i++){
			score += calculateScorePart(i,false); //No Logging
		}
		return (int) Math.round(score+specialScore);
	}

	public String getScores() {
		String tokens = "";
		for (int i = 0; i < QScores.size(); i++) {
			tokens += QScores.get(i).packedString();
			if (i < QScores.size() - 1)
				tokens += ";"; // Skip ; after the last token
		}
		return tokens;
	}

	/**
	 * After getting a random number between 1:TotalStudy,
	 * it's needed to map that number to un-contiguous study part.
	 * @param CntTot: 
	 * @return
	 */
	public QQSparseResult getSparsePoint(int CntTot) {
		int Length = 0, i, pLength;
		for (i = 0; i < QParts.size(); i++) {
			pLength = QParts.get(i).getLength();
			if (CntTot < Length + pLength) {
				return new QQSparseResult(QParts.get(i).getStart() + CntTot
						- Length, i);
			} else {
				Length += pLength;
			}
		}
		return new QQSparseResult(QParts.get(i).getStart(), i);
	}

	public String getStudyParts() {
		String tokens = "";
		QQStudyPart currentPart;
		for (int i = 0; i < QParts.size(); i++) {
			currentPart = QParts.get(i);
			tokens += String.valueOf(currentPart.getStart()) + ",";
			tokens += String.valueOf(currentPart.getNonZeroLength()) + ",";
			tokens += String.valueOf(currentPart.getNumCorrect()) + ",";
			tokens += String.valueOf(currentPart.getNumQuestions()) + ",";
			tokens += String.valueOf(currentPart.getAvgLevel());
			if (i < QParts.size() - 1)
				tokens += ";"; // Skip ; after the last token
		}
		return tokens;
	}

	/**
	 * @return An array of weights for studied parts to sum up 
	 * to QQUtils.DAILYQUIZ_QPERPART_COUNT
	 */
	public int[] getDailyQuizStudyPartsWeights(String dailyRandom){
		int sparse[] = new int[QQUtils.DAILYQUIZ_PARTS_COUNT];
		int totalStudyWeight = (int)Math.ceil((double)getTotalStudyLength()/QQUtils.Juz2AvgWords);
		int Wn, WnX100_remainder=0, WnX100;
		
		/*Fill array as: [0, 0, W1, 0, W2, ..]*/
		for(int i=0;i<QQUtils.DAILYQUIZ_PARTS_COUNT;i++){
			//Skip Al-Fatiha
			if(QParts.get(i+1).getLength()==0)
				sparse[i] = 0;
			else{
				WnX100 = QQUtils.DAILYQUIZ_QPERPART_COUNT*QQUtils.PartWeight100[i+1]
									/totalStudyWeight; //Weight with scaling of 100
				if( Integer.valueOf(dailyRandom.charAt(i))>4 ){
					Wn = (int) Math.ceil((WnX100+WnX100_remainder)/(float)100);
					if(Wn>0){
						sparse[i] = Wn;
						WnX100_remainder += WnX100-100;
					} else {
						sparse[i] = 0;
						WnX100_remainder += WnX100;
					}
				} else {
					Wn = (int) Math.round((WnX100+WnX100_remainder)/(float)100);
					if(Wn>0){
						sparse[i] = Wn;
						WnX100_remainder += WnX100-100;
					} else {
						sparse[i] = 0;
						WnX100_remainder += WnX100;
					}
				}
			}
		}

		/*Check if W1:Wn sum to DAILYQUIZ_QPERPART_COUNT*/
		int sum=0;
		for(int i=0;i<QQUtils.DAILYQUIZ_PARTS_COUNT;i++)
			sum += sparse[i];
		int sumCorrection = QQUtils.DAILYQUIZ_QPERPART_COUNT - sum;
		
		/*Apply correction (due to approx) to last selection*/
		for(int i=QQUtils.DAILYQUIZ_PARTS_COUNT-1;i>=0;i--){
			if(sparse[i]!=0){
				sparse[i] += sumCorrection;
				break;
			}
		}

		Log.i("DailyQuizStudyPartsWeights:: "+ sparse[0] + sparse[1] + sparse[2] + sparse[3] + sparse[4] + sparse[5] 
											 + sparse[6] + sparse[7] + sparse[8] + sparse[9] + sparse[10] + sparse[11] 
											 + sparse[12] + sparse[13] + sparse[14] + sparse[15] + sparse[16] + sparse[17] 
											 + sparse[18] + sparse[19] + sparse[20] + sparse[21] + sparse[22] + sparse[23] 
											 + sparse[24] + sparse[25] + sparse[26] + sparse[27] + sparse[28] + sparse[29] 
											 + sparse[30] + sparse[31] + sparse[32] + sparse[33] + sparse[34] + sparse[35] 
											 + sparse[36] + sparse[37] + sparse[38] + sparse[39] + sparse[40] + sparse[41] 
											 + sparse[42] + sparse[43] + sparse[44] + sparse[45] + sparse[46] + sparse[47] 
										     + " " + sparse[48]);
		
		return sparse;
	}

	public int getTotalCorrect() {
		int Tot = 0;
		QQStudyPart QPart;
		for (int i = 0; i < QParts.size(); i++) {
			QPart = QParts.get(i);
			if (QPart.getStart() > 0)
				Tot += QPart.getNumCorrect();
		}
		return Tot;
	}

	public int getTotalQuesCount() {
		int Tot = 0;
		QQStudyPart QPart;
		for (int i = 0; i < QParts.size(); i++) {
			QPart = QParts.get(i);
			if (QPart.getStart() > 0)
				Tot += QPart.getNumQuestions();
		}
		return Tot;
	}

	/**
	 * @return The total word count of studied parts
	 */
	public int getTotalStudyLength() {
		int Length = 0;
		QQStudyPart QPart;
		for (int i = 0; i < QParts.size(); i++) {
			QPart = QParts.get(i);
			if (QPart.getStart() > 0)
				Length += QPart.getLength();
		}
		return Length;
	}

	public String getuid() {
		return uid;
	}

	public void setLastSeed(int lastSeed) {
		this.lastSeed = lastSeed;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	private void setScoreHistory(String QScoresString) {
		QScores = new Vector<QQScoreRecord>();
		for (String token : QScoresString.split(";")) {
			QScores.add(new QQScoreRecord(token));
		}
	}

	public void setStudyParts(String QPartsString) {
		String[] partElements;

		QParts = new Vector<QQStudyPart>();
		for (String token : QPartsString.split(";")) {
			partElements = token.split(",");
			QParts.add(new QQStudyPart(Integer.parseInt(partElements[0]),
					Integer.parseInt(partElements[1]), 
					Integer.parseInt(partElements[2]),
					Integer.parseInt(partElements[3]),
					Double.parseDouble(partElements[4])));
		}
	}

	public void setuid(String id) {
		uid = id;
	}

	public boolean updateScoreRecord() {
		if (QScores.size() < 5) {
			QScores.add(new QQScoreRecord(this.getScore()));
			return true;
		} else if (QScores.get(QScores.size() - 1).isOlderThan1Day()) {
			QScores.add(new QQScoreRecord(this.getScore()));
			return true;
		}
		return false;
	}

	public double getTotAvgLevel() {
		double avg=0.0,studyWeight=0.0;
		double avgLevel,partWeight;
		for(int i=0;i<QParts.size();i++){
			avgLevel    = QParts.get(i).getAvgLevel();
			partWeight	= QQUtils.PartWeight100[i]/100; 
			studyWeight += partWeight;
			avg 		+= avgLevel*partWeight;
		}
		return avg/studyWeight;
	}

	public void addSpecial(int score) {
		specialScore+= score;		
	}
	
	public void setSpecialScore(int score){
		specialScore = score;
	}
	
	public int getSpecialScore(){
		return specialScore;
	}

	public CharSequence getUpScore(int CurrentPart) {
		double currentPartScore, currentPartScoreUp;
		double partWeight, avgLevel, scaledQCount, scaledCorrectRatio;
		int numCorrect, numQuestions, currLevel;
		
		/**
		 * Find the difference between the current and the to-be-incremented score
		 * The difference comes from the current part only. The current is calculated
		 * normally, while the UP is calculated manually as below
		 * */
		currentPartScore = calculateScorePart(CurrentPart, true);
		
		/**
		 * Calculate the normalized Number of words in current part + UP
		 */
		numCorrect   = QParts.get(CurrentPart).getNumCorrect();
		numQuestions = QParts.get(CurrentPart).getNumQuestions();
		currLevel  	 = getLevel();
		
		partWeight   = QQUtils.PartWeight100[CurrentPart]/100;
		
		avgLevel     = numCorrect*(QParts.get(CurrentPart).getAvgLevel())+currLevel;
		avgLevel	/= (numCorrect+1);		
		
		scaledQCount = QQUtils.sCurve(QParts.get(CurrentPart).getNumCorrect() + 1,
									  QQUtils.Juz2SaturationQCount*partWeight);
		scaledQCount+=1;
		scaledCorrectRatio = QQUtils.sCurve(((float)numCorrect+1)/((float)numQuestions+1),1);
		
		currentPartScoreUp = 100*partWeight*avgLevel*scaledQCount*scaledCorrectRatio;

		return String.valueOf((int)Math.round(currentPartScoreUp-currentPartScore));
	}

	public CharSequence getDownScore(int CurrentPart) {
		double currentPartScore, currentPartScoreDown;
		double partWeight, avgLevel, scaledQCount, scaledCorrectRatio;
		int numCorrect, numQuestions, downScore;
		
		/**
		 * Find the difference between the current and the to-be-decremented score
		 * The difference comes from the current part only. The current is calculated
		 * normally, while the DOWN is calculated manually as below
		 * */
		currentPartScore = calculateScorePart(CurrentPart, false);
		
		/**
		 * Calculate the normalized Number of words in current part + UP
		 */
		numCorrect   = QParts.get(CurrentPart).getNumCorrect();
		numQuestions = QParts.get(CurrentPart).getNumQuestions();
		partWeight   = QQUtils.PartWeight100[CurrentPart]/100;
		
		avgLevel     = QParts.get(CurrentPart).getAvgLevel();		
		scaledQCount = QQUtils.sCurve(QParts.get(CurrentPart).getNumCorrect(),
									  QQUtils.Juz2SaturationQCount*partWeight);
		scaledQCount+=1;
		scaledCorrectRatio = QQUtils.sCurve(((float)numCorrect)/((float)numQuestions+1),1);
		
		currentPartScoreDown = 100*partWeight*avgLevel*scaledQCount*scaledCorrectRatio;
		downScore 			 = (int)Math.round(currentPartScore-currentPartScoreDown);
		if(QQUtils.QQDebug==1 && downScore ==0 ){
			Log.d("[0Down] Sd= "+new DecimalFormat("##.##").format(100*partWeight*avgLevel*scaledQCount*scaledCorrectRatio)+
					"::pW="+new DecimalFormat("##.##").format(partWeight)+
					" av="+new DecimalFormat("##.##").format(avgLevel)+
					" sC="+new DecimalFormat("##.##").format(scaledQCount)+
					" sR="+new DecimalFormat("##.##").format(scaledCorrectRatio));			
		}
		return String.valueOf(downScore);
	}
	
	private double calculateScorePart(int i, boolean enableLogging){
		double score=0.0;
		double partWeight,scaledQCount,avgLevel,scaledCorrectRatio;
		/**
		 * Calculate the normalized Number of words in current part
		 */
		partWeight   = QQUtils.PartWeight100[i]/100;
		avgLevel     = QParts.get(i).getAvgLevel();
		scaledQCount = QQUtils.sCurve(QParts.get(i).getNumCorrect(),
									  QQUtils.Juz2SaturationQCount*partWeight);
		scaledQCount+=1;
		scaledCorrectRatio = QQUtils.sCurve(QParts.get(i).getCorrectRatio(),1);
		
		score = 100*partWeight*avgLevel*scaledQCount*scaledCorrectRatio;
		if(QQUtils.QQDebug==1 && QParts.get(i).getNumCorrect()>0 && enableLogging)
			Log.d("["+i+"] S= "+new DecimalFormat("##.##").format(100*partWeight*avgLevel*scaledQCount*scaledCorrectRatio)+
					"::pW="+new DecimalFormat("##.##").format(partWeight)+
					" av="+new DecimalFormat("##.##").format(avgLevel)+
					" sC="+new DecimalFormat("##.##").format(scaledQCount)+
					" sR="+new DecimalFormat("##.##").format(scaledCorrectRatio));
		return score;
	}
		
	/**
	 * Used to dump values stored statically into QQUtils.PartWeight100 
	 */
	@SuppressWarnings("unused")
	private void dumpWeights(){
		for(int i=0;i<QParts.size();i++)
			System.out.println("["+i+"]= " + Math.round(((double)QParts.get(i).getNonZeroLength()*100)/QQUtils.Juz2AvgWords));
	}
	
	/**
	 * If current profile selections has too few suras, then the user 
	 * is not eligible to some special questions; example: State Sura Name.
	 * @return
	 */
	public boolean isSurasSpecialQuestionEligible(){
		for(int i=QParts.size()-1;i>=QParts.size()-5;i--)
			if(QParts.get(i).isSelected()) return true;
		
		int s=0;
		for(int j=QParts.size()-6;j>0;j--){
			if(QParts.get(j).isSelected()) s++;
			if(s>=QQUtils.SurasSpecialQuestionEligibilityThreshold)
				return true;
		}
		return false; 
	}
}
