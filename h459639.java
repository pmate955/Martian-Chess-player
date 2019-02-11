///HA8MZ_v4
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import game.engine.utils.Pair;
import game.mc.MCAction;
import game.mc.MCGame;
import game.mc.MCPlayer;

public class h459639 extends MCPlayer {

	private int myScore; 										// Az �n pontsz�mom
	private int enemyScore; 									// Az ellenf�l pontsz�ma
	private int[] xDir = { -1, 1, -1, 1 }; 						// Gyalog koordin�t�inak v�ltoz�sai az x tengelyen
	private int[] yDir = { -1, -1, 1, 1 }; 						// Gyalog koordin�t�inak v�ltoz�sai az y tengelyen
	private int[] xDrone = { 0, 1, 0, -1 }; 					// Dr�n koordin�t�inak v�ltoz�sai az x tengelyen
	private int[] yDrone = { -1, 0, 1, 0 }; 					// Dr�n koordin�t�inak v�ltoz�sai az y tengelyen

	/** Konstruktor
	 * @param color a j�t�kos szine
	 * @param board a t�bla t�mb
	 * @param r random seed, nem haszn�lom
	 */
	public h459639(int color, int[][] board, Random r) {
		super(color, board, r);
		myScore = 0;
		enemyScore = 0;
	}

	@Override
	public MCAction getAction(List<Pair<Integer, MCAction>> prevActions) {
		int prevScore = 0;
		MCAction lastStep = null;
		for (Pair<Integer, MCAction> action : prevActions) {
			if (action.second == null) {
				continue;
			}
			boolean samePart = (action.second.x1 < 4 && action.second.x2 < 4)
					|| (4 <= action.second.x1 && 4 <= action.second.x2);
			if (samePart) {
				// merge / move
				board[action.second.x2][action.second.y2] += board[action.second.x1][action.second.y1];
			} else {
				// capture / move
				prevScore = board[action.second.x2][action.second.y2];
				board[action.second.x2][action.second.y2] = board[action.second.x1][action.second.y1];
			}
			board[action.second.x1][action.second.y1] = MCGame.empty;
			lastStep = action.second;
		}
		enemyScore += prevScore;								//^^^^^^^^^^^^^^^^^ Greedyb�l felhaszn�lt ellenf�l el�z� l�p�s�t megl�p�, t�bl�t friss�t� algoritmus eddig
		MCAction actionOne = null;								//A l�p�sem
		if (onlyOne(board)) {									//Ha m�r csak egy b�b�m van
			List<MCAction> option = getStepsForPlayer(board, super.color);		//Akkor azt az ellenf�l t�rfele fel� l�ptetem. 
			if (color == 0) {													//Ezt �gy teszem meg, hogy az x2 koordin�t�n maximaliz�lok vagy minimaliz�lok t�rf�lt�l f�gg�en
				int maxY = 0;
				for (MCAction act : option) {
					if (act.x2 > maxY) {
						maxY = act.x2;
						actionOne = act;
					}
				}
			} else {
				int minY = 8;
				for (MCAction act : option) {
					if (act.x2 < minY) {
						minY = act.x2;
						actionOne = act;
					}
				}
			}

		}
		
		int[] result = new int[6];								//A minimax eredm�ny�nek t�rol�s�ra
		MCAction action;										//A konkr�t l�p�s
		if (actionOne == null) {																	//Ha 1n�l t�bb b�bum van, akkor minimax-ot h�vok
			result = minimaxAlphaBeta(4, super.color, myScore, enemyScore, lastStep, Integer.MIN_VALUE,
					Integer.MAX_VALUE);
			 action = new MCAction(result[1], result[2], result[3], result[4]);						//Az eredm�nyb�l k�sz�l az �j l�p�s
		} else {
			action = actionOne;																		//Ha 1 b�bum van, akkor azt a l�p�st v�lasztom, amit fentebb kerestem
			result[5] = 0;																			//Nem mergelek
		}
		int addScore = board[action.x2][action.y2];
		if(result[5] == 1 && board[action.x2][action.y2] != MCGame.empty) {							//Hanem �res az �j mez�
		   board[action.x2][action.y2] += board[action.x1][action.y1];								//Akkor az �j mez�h�z hozz�adja a m�sik mez� �rt�k�t - mergel
		} else {
			board[action.x2][action.y2] = board[action.x1][action.y1];								//Ha �res a mez�, akkor szimpl�n �trakja az �rt�ket
		   myScore += addScore;
		}																							
		board[action.x1][action.y1] = MCGame.empty;
		
		return action;

	}

	/**
	 * A f�ggv�ny ellen�rzi, hogy csak egy b�bum maradt-e. Erre a "csiki-csukiz�s"
	 * elker�l�se v�gett van sz�ks�g.
	 * 
	 * @param board 2D t�bla
	 * @return Logikai �rt�k, ha true, akkor csak egy b�bu maradt
	 */
	private boolean onlyOne(int[][] board) {
		int num = 0; 															// A tal�lt b�buk sz�ma
		for (int i = 4 * color; i < 4 * (color + 1); i++) { 					// V�gigmegy a sorokon, ha a color 0, akkor a fels� j�t�kost
																				// n�zi, ha 1, akkor az als�t
			for (int j = 0; j < 4; j++) {
				if (board[i][j] != 0)
					num++; 														// Ha tal�lt b�but a saj�t t�rf�len, n�veli a v�ltoz�t
			}
		}
		return num == 1; 														// Visszat�r�si �rt�k egy logikai vizsg�lat �rt�ke
	}

	/**Alfa-b�ta v�g�ssal jav�tott minmax algoritmus megval�s�t�sa. 
	 * @param depth vizsg�lt maxim�lis m�lys�g
	 * @param color a j�t�kos sz�ne (mindig az�, aki k�vetkezik)
	 * @param playerScore a p�ld�nyom pontsz�ma (fixen az eny�m)
	 * @param enemyScore az ellenf�l pontsz�ma
	 * @param lastStep a legut�bbi l�p�s action, az ellenf�l l�p�sism�tl�s�nek elker�l�s�re
	 * @param alpha alfa �rt�ke
	 * @param beta b�ta �rt�ke
	 * @return int t�mb�t ad vissza, melyben szerepel az el�rt pontsz�m, valamint a
	 *         legjobb l�p�s 4 koordin�t�ja
	 */
	private int[] minimaxAlphaBeta(int depth, int color, int playerScore, int enemyScore, MCAction lastStep, int alpha,
			int beta) {
		List<MCAction> actions = getStepsForPlayer(board, color);							//Lek�rj�k a j�t�kos l�p�seit
		int bestScore = (color == super.color) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
		int currentScore;
		int useMerge = 0;																	//Haszn�lunk e b�buegyes�t�st 0 = nem, 1 = igen
		MCAction bestStep = null;															//Legjobb l�p�s
		if (depth == 0 || actions.isEmpty()) { 												// Ha el�g m�lyen vagyunk, visszaadom a pontok �tlag�t. Ha >50, akkor mi nyert�nk, ha nem, akkor nem
			bestScore = (int) (((double) playerScore / enemyScore) * 100); 					// Egy �tlagot adok vissza
			return new int[] { bestScore, -1, -1, -1, -1, -1 };								//A l�p�s -1, mert nem lehet tov�bb menni

		} else {
			for (MCAction act : actions) {													//V�gigmegyek a lehets�ges l�p�seken
				if (lastStep != null && (act.x1 == lastStep.x2 && act.y1 == lastStep.y2 && lastStep.x1 == act.x2
						&& lastStep.y1 == act.y2)) {										//Nem l�phetem ugyanazt, amit az ellenf�l
					continue;
				}
				int last1 = board[act.x1][act.y1];											//Let�rolom mi volt a forr�s �s c�l koordin�t�n a vissza�ll�t�s miatt
				int new1 = board[act.x2][act.y2];
				int localMerge = 0;
				if(isOnSameSide(act.x2, color) && board[act.x2][act.y2] != MCGame.empty) {			///Ha nem �res az �j mez�
				   board[act.x2][act.y2] += board[act.x1][act.y1];								//Akkor mergel�nk
				   localMerge = 1;
				} else {
				   board[act.x2][act.y2] = board[act.x1][act.y1];								//Ha �res a m�sik mez�, akkor szimpl�n �trakja az �rt�ket
				}				
				board[act.x1][act.y1] = 0;														//A kiindul� helyet kinull�zom
				if (color == super.color) {													//Az �n j�t�kosom (A maximaliz�l� j�t�kos)
					if(localMerge == 0) playerScore += new1;								//Csak akkor adom hozz� a pontokhoz az �j mez� �rt�k�t, ha nem mergel�s volt 
					currentScore = minimaxAlphaBeta(depth - 1, getEnemyIndex(color), playerScore, enemyScore, act,
							alpha, beta)[0];													//Rekurzi� a f�ban lejjebb l�p�shez, az �j pont�rt�kkel
					if (currentScore > alpha) {
						alpha = currentScore;													//Ha jobb a pont, mint az alfa, akkor elt�roljuk a l�p�st, �s friss�tj�k alfa �rt�k�t
						bestStep = act;
						useMerge = localMerge;
					}
					if(localMerge == 0) playerScore -= new1;								//Csak akkor vonom vissza a pontot, ha nem mergel�s volt
				} else {																			//Ellenf�l j�t�kosa (minimaliz�l�s)
					if(localMerge == 0) enemyScore += new1;											//Teljesen anal�g a m�sik �ghoz k�pest, csak a b�t�ra, �s minimumot keres
					currentScore = minimaxAlphaBeta(depth - 1, getEnemyIndex(color), playerScore, enemyScore, act,
							alpha, beta)[0];
					if (currentScore < beta) {
						beta = currentScore;
						bestStep = act;
						useMerge = localMerge;
					}
					if(localMerge == 0) enemyScore -= new1;
				}																			//Visszavonom a l�p�st
				board[act.x1][act.y1] = last1;
				board[act.x2][act.y2] = new1;
				if (alpha >= beta)															//Az alfa-b�ta v�g�s bek�vetkez�se
					break;
			}
		}
		if (bestStep == null) {																//Ha nem tal�ltunk jobb l�p�st, akkor visszaadjuk a -1-et
			return new int[] { (super.color == color) ? alpha : beta, -1, -1, -1, -1, -1};
		}
		return new int[] { (super.color == color) ? alpha : beta, bestStep.x1, bestStep.y1, bestStep.x2, bestStep.y2, useMerge };				//Visszaadjuk a l�p�st
	}

	/**
	 * @param player a j�t�kos sz�ne
	 * @return a sz�n ellent�te
	 */
	private int getEnemyIndex(int player) {
		if (player == 0)
			return 1;
		return 0;
	}

	/**
	 * A f�ggv�ny feladata egy lista k�sz�t�se a lehets�ges l�p�sekr�l az adott
	 * j�t�kos szerint
	 * 
	 * @param board t�bla
	 * @param color j�t�kos sz�ne
	 * @return lista, mely t�rolja a lehets�ges l�p�s objektumokat
	 */
	private List<MCAction> getStepsForPlayer(int[][] board, int color) {

		List<MCAction> output = new ArrayList<MCAction>();
		boolean isEmpty = true;
		for (int i = 0; i < 4; i++) { 									// Ha az egyik t�rf�l �res, akkor �res list�t adok vissza, mert v�ge a j�t�knak
			for (int j = 0; j < 4; j++) {
				if (board[i][j] != 0) {
					isEmpty = false;
					break;
				}
			}
		}
		if (isEmpty) {
			return output;
		}
		isEmpty = true;
		for (int i = 4; i < 8; i++) { 									// A m�sik t�rf�l �ress�ge
			for (int j = 0; j < 4; j++) {
				if (board[i][j] != 0) {
					isEmpty = false;
					break;
				}
			}
		}
		if (isEmpty) {
			return output;
		}

		for (int i = 4 * color; i < 4 * (color + 1); i++) {				// V�gigmegy a sorokon, ha a color 0, akkor a fels� j�t�kost n�zi, ha 1, akkor az als�t
			for (int j = 0; j < 4; j++) { 								// V�gigmegy az oszlopokon
				if (board[i][j] != MCGame.empty) { 						// Ha tal�l b�b�t, amivel l�phetne:
					if (board[i][j] == 1) { 							// Ha gyalogunk van, azzal �tl�san l�phet�nk
						for (int iD = 0; iD < xDir.length; iD++) {
							int newI = i + xDir[iD]; 					// Az �j koordin�t�k, melyekhez az elmozdul�st 2 t�mb seg�ts�g�vel kapom
							int newJ = j + yDir[iD];
							if (isValidStep(newI, newJ, color, board, 1)) { 				// Ellen�rz�m az �j koordin�t�kat
								MCAction newAction = new MCAction(i, j, newI, newJ);
								output.add(newAction); 									// Ha j�k, mehetnek a list�ba
							}
						}
					} else if (board[i][j] == 2) { 									// Dr�n l�p�sei
						for (int iD = 0; iD < xDrone.length; iD++) { 				// 4 ir�nyba
							int newI = i, newJ = j;
							for (int delta = 1; delta <= 2; delta++) {				// max 2 l�p�st
								newI += xDrone[iD];
								newJ += yDrone[iD];
								if (isValidStep(newI, newJ, color, board, 2)) {
									output.add(new MCAction(i, j, newI, newJ));
									if (board[newI][newJ] != 0) { 					// Ha le�t�nk valamit, nem mehet�nk tov�bb, azaz nem
																					// ugorhatunk �t semmit
										break;
									}
								} else {
									break;
								}
							}
						}
					} else if (board[i][j] == 3) { 									// Kir�lyn� l�p�se
						for (int iD = 0; iD < xDrone.length; iD++) { 				// 4 v�zszintes ir�nyba
							int newI = i, newJ = j;
							for (int delta = 1; delta <= 8; delta++) { 				// max 8 l�p�s lehet, legalulr�l legfel�lre
								newI += xDrone[iD];
								newJ += yDrone[iD];
								if (isValidStep(newI, newJ, color, board, 3)) {
									output.add(new MCAction(i, j, newI, newJ));
									if (board[newI][newJ] != 0) { 					// Ha le�t�nk valamit, nem mehet�nk tov�bb
										break;
									}
								} else {
									break;
								}
							}
						}
						for (int iD = 0; iD < xDir.length; iD++) { 					// Keresztbe l�p�sek
							int newI = i;
							int newJ = j;
							for (int delta = 1; delta <= 8; delta++) { 				// max 8 l�p�s lehet, legalulr�l legfel�lre
								newI += xDir[iD];
								newJ += yDir[iD];
								if (isValidStep(newI, newJ, color, board, 3)) {
									output.add(new MCAction(i, j, newI, newJ));
									if (board[newI][newJ] != 0) { 					// Ha le�t�nk valamit, nem mehet�nk tov�bb
										break;
									}
								} else {
									break;
								}
							}
						}
					}
				}
			}
		}
		return output;
	}

	/**
	 * Leellen�rzi, hogy a l�p�s�nkkel a p�ly�n bel�l maradunk-e, valamint hogy a
	 * saj�t t�rfel�nk�n nem l�p�nk-e egy m�sik b�bura.
	 * 
	 * @param x     a c�l x koordin�t�ja
	 * @param y     a c�l y koordin�t�ja
	 * @param color a l�p� j�t�kos sz�ne
	 * @param board t�bla
	 * @param value a l�p b�bu �rt�ke
	 * @return logikai �rt�k, a l�p�s j� helyen van-e
	 */
	private boolean isValidStep(int x, int y, int color, int[][] board, int value) {
		if (x < 0 || y < 0 || x >= 8 || y >= 4) { 							// A t�bl�n k�v�lre l�p�nk, akkor false
			return false;
		}
		if (color == 0 && x < 4) { 											// Ha a fels� r�szen vagyunk, �s van rajta valami, az probl�ma
			if(board[x][y] == 1 && value == 1 && !hasDrone(color)) {		//Ha nincs dr�n, l�trehozhatunk egyet
				return true;
			}
			if(value + board[x][y] == 3 && !hasQueen(color)) {				//Ha nincs kir�lyn�, k�sz�thet�nk egyet
				return true;
			}
			if (board[x][y] != 0)
				return false;
		}
		if (color == 1 && x >= 4) { 										// Ha az als� r�szen vagyunk, szint�n nem l�phet�nk a saj�t b�bunkra
			if(board[x][y] == 1 && value == 1 && !hasDrone(color)) {
				return true;
			}
			if(value + board[x][y] == 3 && !hasQueen(color)) {
				return true;
			}
			if (board[x][y] != 0)
				return false;
		}
		return true; 														// Ha egyik felt�tel sem s�r�lt, visszat�r�nk egy true-val
	}

	/**
	 * @param color a j�t�kos sz�ne
	 * @return az adott sz�n� j�t�kosnak van-e dr�nja
	 */
	private boolean hasDrone(int color) {		
		for (int i = 4 * color; i < 4 * (color + 1); i++) {				// V�gigmegy a sorokon, ha a color 0, akkor a fels� j�t�kost n�zi, ha 1, akkor az als�t
			for (int j = 0; j < 4; j++) { 		
				if(board[i][j] == 2) return true;
			}
		}
		return false;
	}
	
	/**
	 * @param color a j�t�kos sz�ne
	 * @return az adott j�t�kosnak van-e kir�lyn�je
	 */
	private boolean hasQueen(int color) {
		for (int i = 4 * color; i < 4 * (color + 1); i++) {				// V�gigmegy a sorokon, ha a color 0, akkor a fels� j�t�kost n�zi, ha 1, akkor az als�t
			for (int j = 0; j < 4; j++) { 		
				if(board[i][j] == 3) return true;
			}
		}
		return false;
	}
	
	/**
	 * @param x j�t�kos x koordin�t�ja
	 * @param color j�t�kos sz�ne
	 * @return a j�t�kos a l�p�se ut�n is a saj�t t�rfel�n van-e
	 */
	private boolean isOnSameSide(int x, int color) {
		if(color == 0) {
			return x < 4;
		}
		return x >= 4;
	}
}
