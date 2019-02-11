///HA8MZ_v4
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import game.engine.utils.Pair;
import game.mc.MCAction;
import game.mc.MCGame;
import game.mc.MCPlayer;

public class h459639 extends MCPlayer {

	private int myScore; 										// Az én pontszámom
	private int enemyScore; 									// Az ellenfél pontszáma
	private int[] xDir = { -1, 1, -1, 1 }; 						// Gyalog koordinátáinak változásai az x tengelyen
	private int[] yDir = { -1, -1, 1, 1 }; 						// Gyalog koordinátáinak változásai az y tengelyen
	private int[] xDrone = { 0, 1, 0, -1 }; 					// Drón koordinátáinak változásai az x tengelyen
	private int[] yDrone = { -1, 0, 1, 0 }; 					// Drón koordinátáinak változásai az y tengelyen

	/** Konstruktor
	 * @param color a játékos szine
	 * @param board a tábla tömb
	 * @param r random seed, nem használom
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
		enemyScore += prevScore;								//^^^^^^^^^^^^^^^^^ Greedybõl felhasznált ellenfél elõzõ lépését meglépõ, táblát frissítõ algoritmus eddig
		MCAction actionOne = null;								//A lépésem
		if (onlyOne(board)) {									//Ha már csak egy bábúm van
			List<MCAction> option = getStepsForPlayer(board, super.color);		//Akkor azt az ellenfél térfele felé léptetem. 
			if (color == 0) {													//Ezt úgy teszem meg, hogy az x2 koordinátán maximalizálok vagy minimalizálok térféltõl függõen
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
		
		int[] result = new int[6];								//A minimax eredményének tárolására
		MCAction action;										//A konkrét lépés
		if (actionOne == null) {																	//Ha 1nél több bábum van, akkor minimax-ot hívok
			result = minimaxAlphaBeta(4, super.color, myScore, enemyScore, lastStep, Integer.MIN_VALUE,
					Integer.MAX_VALUE);
			 action = new MCAction(result[1], result[2], result[3], result[4]);						//Az eredménybõl készül az új lépés
		} else {
			action = actionOne;																		//Ha 1 bábum van, akkor azt a lépést választom, amit fentebb kerestem
			result[5] = 0;																			//Nem mergelek
		}
		int addScore = board[action.x2][action.y2];
		if(result[5] == 1 && board[action.x2][action.y2] != MCGame.empty) {							//Hanem üres az új mezõ
		   board[action.x2][action.y2] += board[action.x1][action.y1];								//Akkor az új mezõhöz hozzáadja a másik mezõ értékét - mergel
		} else {
			board[action.x2][action.y2] = board[action.x1][action.y1];								//Ha üres a mezõ, akkor szimplán átrakja az értéket
		   myScore += addScore;
		}																							
		board[action.x1][action.y1] = MCGame.empty;
		
		return action;

	}

	/**
	 * A függvény ellenõrzi, hogy csak egy bábum maradt-e. Erre a "csiki-csukizás"
	 * elkerülése végett van szükség.
	 * 
	 * @param board 2D tábla
	 * @return Logikai érték, ha true, akkor csak egy bábu maradt
	 */
	private boolean onlyOne(int[][] board) {
		int num = 0; 															// A talált bábuk száma
		for (int i = 4 * color; i < 4 * (color + 1); i++) { 					// Végigmegy a sorokon, ha a color 0, akkor a felsõ játékost
																				// nézi, ha 1, akkor az alsót
			for (int j = 0; j < 4; j++) {
				if (board[i][j] != 0)
					num++; 														// Ha talált bábut a saját térfélen, növeli a változót
			}
		}
		return num == 1; 														// Visszatérési érték egy logikai vizsgálat értéke
	}

	/**Alfa-béta vágással javított minmax algoritmus megvalósítása. 
	 * @param depth vizsgált maximális mélység
	 * @param color a játékos színe (mindig azé, aki következik)
	 * @param playerScore a példányom pontszáma (fixen az enyém)
	 * @param enemyScore az ellenfél pontszáma
	 * @param lastStep a legutóbbi lépés action, az ellenfél lépésismétlésének elkerülésére
	 * @param alpha alfa értéke
	 * @param beta béta értéke
	 * @return int tömböt ad vissza, melyben szerepel az elért pontszám, valamint a
	 *         legjobb lépés 4 koordinátája
	 */
	private int[] minimaxAlphaBeta(int depth, int color, int playerScore, int enemyScore, MCAction lastStep, int alpha,
			int beta) {
		List<MCAction> actions = getStepsForPlayer(board, color);							//Lekérjük a játékos lépéseit
		int bestScore = (color == super.color) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
		int currentScore;
		int useMerge = 0;																	//Használunk e bábuegyesítést 0 = nem, 1 = igen
		MCAction bestStep = null;															//Legjobb lépés
		if (depth == 0 || actions.isEmpty()) { 												// Ha elég mélyen vagyunk, visszaadom a pontok átlagát. Ha >50, akkor mi nyertünk, ha nem, akkor nem
			bestScore = (int) (((double) playerScore / enemyScore) * 100); 					// Egy átlagot adok vissza
			return new int[] { bestScore, -1, -1, -1, -1, -1 };								//A lépés -1, mert nem lehet tovább menni

		} else {
			for (MCAction act : actions) {													//Végigmegyek a lehetséges lépéseken
				if (lastStep != null && (act.x1 == lastStep.x2 && act.y1 == lastStep.y2 && lastStep.x1 == act.x2
						&& lastStep.y1 == act.y2)) {										//Nem léphetem ugyanazt, amit az ellenfél
					continue;
				}
				int last1 = board[act.x1][act.y1];											//Letárolom mi volt a forrás és cél koordinátán a visszaállítás miatt
				int new1 = board[act.x2][act.y2];
				int localMerge = 0;
				if(isOnSameSide(act.x2, color) && board[act.x2][act.y2] != MCGame.empty) {			///Ha nem üres az új mezõ
				   board[act.x2][act.y2] += board[act.x1][act.y1];								//Akkor mergelünk
				   localMerge = 1;
				} else {
				   board[act.x2][act.y2] = board[act.x1][act.y1];								//Ha üres a másik mezõ, akkor szimplán átrakja az értéket
				}				
				board[act.x1][act.y1] = 0;														//A kiinduló helyet kinullázom
				if (color == super.color) {													//Az én játékosom (A maximalizáló játékos)
					if(localMerge == 0) playerScore += new1;								//Csak akkor adom hozzá a pontokhoz az új mezõ értékét, ha nem mergelés volt 
					currentScore = minimaxAlphaBeta(depth - 1, getEnemyIndex(color), playerScore, enemyScore, act,
							alpha, beta)[0];													//Rekurzió a fában lejjebb lépéshez, az új pontértékkel
					if (currentScore > alpha) {
						alpha = currentScore;													//Ha jobb a pont, mint az alfa, akkor eltároljuk a lépést, és frissítjük alfa értékét
						bestStep = act;
						useMerge = localMerge;
					}
					if(localMerge == 0) playerScore -= new1;								//Csak akkor vonom vissza a pontot, ha nem mergelés volt
				} else {																			//Ellenfél játékosa (minimalizálás)
					if(localMerge == 0) enemyScore += new1;											//Teljesen analóg a másik ághoz képest, csak a bétára, és minimumot keres
					currentScore = minimaxAlphaBeta(depth - 1, getEnemyIndex(color), playerScore, enemyScore, act,
							alpha, beta)[0];
					if (currentScore < beta) {
						beta = currentScore;
						bestStep = act;
						useMerge = localMerge;
					}
					if(localMerge == 0) enemyScore -= new1;
				}																			//Visszavonom a lépést
				board[act.x1][act.y1] = last1;
				board[act.x2][act.y2] = new1;
				if (alpha >= beta)															//Az alfa-béta vágás bekövetkezése
					break;
			}
		}
		if (bestStep == null) {																//Ha nem találtunk jobb lépést, akkor visszaadjuk a -1-et
			return new int[] { (super.color == color) ? alpha : beta, -1, -1, -1, -1, -1};
		}
		return new int[] { (super.color == color) ? alpha : beta, bestStep.x1, bestStep.y1, bestStep.x2, bestStep.y2, useMerge };				//Visszaadjuk a lépést
	}

	/**
	 * @param player a játékos színe
	 * @return a szín ellentéte
	 */
	private int getEnemyIndex(int player) {
		if (player == 0)
			return 1;
		return 0;
	}

	/**
	 * A függvény feladata egy lista készítése a lehetséges lépésekrõl az adott
	 * játékos szerint
	 * 
	 * @param board tábla
	 * @param color játékos színe
	 * @return lista, mely tárolja a lehetséges lépés objektumokat
	 */
	private List<MCAction> getStepsForPlayer(int[][] board, int color) {

		List<MCAction> output = new ArrayList<MCAction>();
		boolean isEmpty = true;
		for (int i = 0; i < 4; i++) { 									// Ha az egyik térfél üres, akkor üres listát adok vissza, mert vége a játéknak
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
		for (int i = 4; i < 8; i++) { 									// A másik térfél üressége
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

		for (int i = 4 * color; i < 4 * (color + 1); i++) {				// Végigmegy a sorokon, ha a color 0, akkor a felsõ játékost nézi, ha 1, akkor az alsót
			for (int j = 0; j < 4; j++) { 								// Végigmegy az oszlopokon
				if (board[i][j] != MCGame.empty) { 						// Ha talál bábút, amivel léphetne:
					if (board[i][j] == 1) { 							// Ha gyalogunk van, azzal átlósan léphetünk
						for (int iD = 0; iD < xDir.length; iD++) {
							int newI = i + xDir[iD]; 					// Az új koordináták, melyekhez az elmozdulást 2 tömb segítségével kapom
							int newJ = j + yDir[iD];
							if (isValidStep(newI, newJ, color, board, 1)) { 				// Ellenõrzöm az új koordinátákat
								MCAction newAction = new MCAction(i, j, newI, newJ);
								output.add(newAction); 									// Ha jók, mehetnek a listába
							}
						}
					} else if (board[i][j] == 2) { 									// Drón lépései
						for (int iD = 0; iD < xDrone.length; iD++) { 				// 4 irányba
							int newI = i, newJ = j;
							for (int delta = 1; delta <= 2; delta++) {				// max 2 lépést
								newI += xDrone[iD];
								newJ += yDrone[iD];
								if (isValidStep(newI, newJ, color, board, 2)) {
									output.add(new MCAction(i, j, newI, newJ));
									if (board[newI][newJ] != 0) { 					// Ha leütünk valamit, nem mehetünk tovább, azaz nem
																					// ugorhatunk át semmit
										break;
									}
								} else {
									break;
								}
							}
						}
					} else if (board[i][j] == 3) { 									// Királynõ lépése
						for (int iD = 0; iD < xDrone.length; iD++) { 				// 4 vízszintes irányba
							int newI = i, newJ = j;
							for (int delta = 1; delta <= 8; delta++) { 				// max 8 lépés lehet, legalulról legfelülre
								newI += xDrone[iD];
								newJ += yDrone[iD];
								if (isValidStep(newI, newJ, color, board, 3)) {
									output.add(new MCAction(i, j, newI, newJ));
									if (board[newI][newJ] != 0) { 					// Ha leütünk valamit, nem mehetünk tovább
										break;
									}
								} else {
									break;
								}
							}
						}
						for (int iD = 0; iD < xDir.length; iD++) { 					// Keresztbe lépések
							int newI = i;
							int newJ = j;
							for (int delta = 1; delta <= 8; delta++) { 				// max 8 lépés lehet, legalulról legfelülre
								newI += xDir[iD];
								newJ += yDir[iD];
								if (isValidStep(newI, newJ, color, board, 3)) {
									output.add(new MCAction(i, j, newI, newJ));
									if (board[newI][newJ] != 0) { 					// Ha leütünk valamit, nem mehetünk tovább
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
	 * Leellenõrzi, hogy a lépésünkkel a pályán belül maradunk-e, valamint hogy a
	 * saját térfelünkön nem lépünk-e egy másik bábura.
	 * 
	 * @param x     a cél x koordinátája
	 * @param y     a cél y koordinátája
	 * @param color a lépõ játékos színe
	 * @param board tábla
	 * @param value a lép bábu értéke
	 * @return logikai érték, a lépés jó helyen van-e
	 */
	private boolean isValidStep(int x, int y, int color, int[][] board, int value) {
		if (x < 0 || y < 0 || x >= 8 || y >= 4) { 							// A táblán kívülre lépünk, akkor false
			return false;
		}
		if (color == 0 && x < 4) { 											// Ha a felsõ részen vagyunk, és van rajta valami, az probléma
			if(board[x][y] == 1 && value == 1 && !hasDrone(color)) {		//Ha nincs drón, létrehozhatunk egyet
				return true;
			}
			if(value + board[x][y] == 3 && !hasQueen(color)) {				//Ha nincs királynõ, készíthetünk egyet
				return true;
			}
			if (board[x][y] != 0)
				return false;
		}
		if (color == 1 && x >= 4) { 										// Ha az alsó részen vagyunk, szintén nem léphetünk a saját bábunkra
			if(board[x][y] == 1 && value == 1 && !hasDrone(color)) {
				return true;
			}
			if(value + board[x][y] == 3 && !hasQueen(color)) {
				return true;
			}
			if (board[x][y] != 0)
				return false;
		}
		return true; 														// Ha egyik feltétel sem sérült, visszatérünk egy true-val
	}

	/**
	 * @param color a játékos színe
	 * @return az adott színû játékosnak van-e drónja
	 */
	private boolean hasDrone(int color) {		
		for (int i = 4 * color; i < 4 * (color + 1); i++) {				// Végigmegy a sorokon, ha a color 0, akkor a felsõ játékost nézi, ha 1, akkor az alsót
			for (int j = 0; j < 4; j++) { 		
				if(board[i][j] == 2) return true;
			}
		}
		return false;
	}
	
	/**
	 * @param color a játékos színe
	 * @return az adott játékosnak van-e királynõje
	 */
	private boolean hasQueen(int color) {
		for (int i = 4 * color; i < 4 * (color + 1); i++) {				// Végigmegy a sorokon, ha a color 0, akkor a felsõ játékost nézi, ha 1, akkor az alsót
			for (int j = 0; j < 4; j++) { 		
				if(board[i][j] == 3) return true;
			}
		}
		return false;
	}
	
	/**
	 * @param x játékos x koordinátája
	 * @param color játékos színe
	 * @return a játékos a lépése után is a saját térfelén van-e
	 */
	private boolean isOnSameSide(int x, int color) {
		if(color == 0) {
			return x < 4;
		}
		return x >= 4;
	}
}
