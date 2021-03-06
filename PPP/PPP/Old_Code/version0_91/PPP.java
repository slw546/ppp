package version0_91;
import java.util.Random;
/*
 * 	Author: Hao Wei
 * 	Time:	14/05/2013
 * 	Description: PPP(size, lDes, nDes)
 * 	Notes:	In version 0.5, the vector representing the taxonomic character
 * 			can be created by the PPP class.
 * 			In version 0.6, the descriptor can be stored in the arrayDes of PPP.
 * 			In version 0.6, the arrayDes can be mutated.
 * 			In version 0.6, PPP can regenerate the length of a single descriptor.
 * 			In version 0.7, the maximum number of obstructions is added(replacing lDes)
 * 			In version 0.7, the length of the descriptor is generated by averaging two random numbers
 * 			In version 0.9, PPP(PPP ppp) is using copyPPP()!
 * 	Next:	change PPP for new generator of descriptor
 * 			*** generate the length of the descriptor in iniDescriptors
 * 			don't occ in draw map
 */

public class PPP{
	private short size;	// the size of the PPP
	private short nDes;	// the number of descriptors
	private short maxObs;	// the maximum number of obstructions allowed
	private short row;	// the number of chars in one column
	private short col;	// the number of chars in one row
	private char[][] map;	// the char array of the map
	private Descriptor[] arrayDes;	// the array of the descriptors
	private Descriptor[] tempDes;	// the temp array of the descriptors, for updatePPP
	/*
	 * 	0 presents the current cell is non-occupied and 1 presents occupied
	 * 	2 presents the cell is occupied by the agent or the destination
	 */
	private short[][] occ; 
	/*
	 * 	The state space array of all possible triples (x,y,h) that the agent can occupy
	 */
	private AgentState[] asArray;
	private short arraySize;			// the size of the array of agent state
	private boolean availability;	// the availability of the PPP, whether it is reachable or not
	private StateValue bestSV;		// the best StateValue
	private AgentState finalAS; 	// the final AgentState
	/*
	 * 	Initialize the map
	 */
	public PPP(short size, short nDes, short maxObs){
		this.size = size;
		this.nDes = nDes;
		this.maxObs = maxObs;
		row = (short)(size+2);
		col = (short)(size*2+2);
		map = new char[row][col];
		occ = new short[row][col];
		iniPPP();
	}
	/*
	 * 	Initialize the map using a given PPP
	 */
	public PPP(PPP ppp){
		this.size = ppp.size;
		this.nDes = ppp.nDes;
		this.maxObs = ppp.maxObs;
		this.arraySize = ppp.arraySize;
		row = (short)(size+2);
		col = (short)(size*2+2);
		map = new char[row][col];
		occ = new short[row][col];
		arrayDes = new Descriptor[nDes];
		System.arraycopy(ppp.arrayDes, 0, this.arrayDes, 0, ppp.arrayDes.length);
		copyPPP();
	}
	/*
	 * initialize the occ array
	 */
	private void iniOcc(){
		for (short i = 0; i<row; i++){
			for (short j = 0; j<col; j++){
				occ[i][j] = 0;
			}
		}
	}
	/*
	 * 	Draw the map
	 */
	public void drawMap(){
		iniOcc();
		drawBoundary();
		drawAgency();
		drawDestination();
		drawDescriptors();
		drawDot();
	}
	/*
	 * 	Draw the boundaries for the map
	 */
	private void drawBoundary(){
		for (int i = 0; i<col; i++){
			map[0][i] = '#';
			occ[0][i] = 1;
		}
		for (int i = 1; i< row-1; i++){
			map[i][0] = '#';
			occ[i][0] = 1;
		}
		for (int i = 1; i<row-1; i++){
			map[i][col-1] = '#';
			occ[i][col-1] = 1;
		}
		for (int i = 0; i<col; i++){
			map[row-1][i] = '#';
			occ[row-1][i] = 1;
		}
	}
	/*
	 *	Draw the agency in the initial position
	 */
	private void drawAgency(){
		map[1][1] = '-';
		map[1][2] = '>';
		occ[1][1] = 2;
		occ[1][2] = 2;
	}
	/*
	 * 	Draw the destination for the agency
	 */
	private void drawDestination(){
		map[row-2][col-2] = '*';
		map[row-2][col-3] = '*';
		occ[row-2][col-2] = 2;
		occ[row-2][col-3] = 2;
	}
	/*
	 * 	Draw all the descriptors
	 */
	private void drawDescriptors(){
		for (short i = 0; i<nDes; i++){
			// the x position of the descriptor
			short rRow = arrayDes[i].getX();
			// the y position of the descriptor
			short rCol = arrayDes[i].getY();
			// the type of the descriptor, totally six.
			short type = arrayDes[i].getType();
			if(occ[rRow][rCol]==0){
				map[rRow][rCol] = '[';
				map[rRow][rCol+1] = ']';
				occ[rRow][rCol] = 1;
				occ[rRow][rCol+1] = 1;
			}
			// the maximum number of obstructions for one descriptor
			// the length of the descriptor is determined in iniDescriptors
			short maxObs = arrayDes[i].getLength();
			// 0 presents right
			if (type == 0){ 
				for (short j=0; j<maxObs; j++){
					if(rCol+j*2<col-2){
						if(occ[rRow][rCol+j*2]==0){
							map[rRow][rCol+j*2] = '[';
							map[rRow][rCol+1+j*2] = ']';
							occ[rRow][rCol+j*2] = 1;
							occ[rRow][rCol+1+j*2] = 1;
						} else maxObs++;
					}
				}
			}
			// 1 presents left
			if (type == 1){
				for (short j=0; j<maxObs; j++){
					if(rCol-j*2>0){
						if(occ[rRow][rCol-j*2]==0){
							map[rRow][rCol-j*2] = '[';
							map[rRow][rCol+1-j*2] = ']';
							occ[rRow][rCol-j*2] = 1;
							occ[rRow][rCol+1-j*2] = 1;
						} else maxObs++;
					}
				}
			}
			// 2 presents up
			if (type == 2){
				for (short j=0; j<maxObs; j++){
					if(rRow-j>0){
						if(occ[rRow-j][rCol]==0){
							map[rRow-j][rCol] = '[';
							map[rRow-j][rCol+1] = ']';
							occ[rRow-j][rCol] = 1;
							occ[rRow-j][rCol+1] = 1;
						} else maxObs++;
					}
				}
			}
			// 3 presents down
			if (type == 3){
				for (short j=0; j<maxObs; j++){
					if(rRow+j<row-1){
						if(occ[rRow+j][rCol]==0){
							map[rRow+j][rCol] = '[';
							map[rRow+j][rCol+1] = ']';
							occ[rRow+j][rCol] = 1;
							occ[rRow+j][rCol+1] = 1;
						} else maxObs++;
					}
				}
			}
			// 4 presents left-up
			if (type == 4){
				for (short j=0; j<maxObs; j++){
					if(rCol-j*2>0 && rRow+j<row-1){
						if(occ[rRow+j][rCol-j*2]==0){
							map[rRow+j][rCol-j*2] = '[';
							map[rRow+j][rCol+1-j*2] = ']';
							occ[rRow+j][rCol-j*2] = 1;
							occ[rRow+j][rCol+1-j*2] = 1;
						} else maxObs++;
					}
				}
			}
			// 5 presents left-down
			if (type == 5){
				for (short j=0; j<maxObs; j++){
					if(rCol-j*2>0 && rRow-j>0){
						if(occ[rRow-j][rCol-j*2]==0){
							map[rRow-j][rCol-j*2] = '[';
							map[rRow-j][rCol+1-j*2] = ']';
							occ[rRow-j][rCol-j*2] = 1;
							occ[rRow-j][rCol+1-j*2] = 1;
						} else maxObs++;
					}
				}
			}
		}
	}
	/*
	 * 	Draw the dot for the map
	 */
	private void drawDot(){
		for (short i = 0; i<row; i++){
			for (short j = 0; j<col; j++){
				if(occ[i][j]==0){
					map[i][j] = '.';
				}
			}
		}
	}
	/*
	 * 	initialize boundaries for createDescriptors
	 */
	private void iniBoundary(){
		for (short i = 0; i<col; i++){
			occ[0][i] = 1;
		}
		for (short i = 1; i< row-1; i++){
			occ[i][0] = 1;
		}
		for (short i = 1; i<row-1; i++){
			occ[i][col-1] = 1;
		}
		for (short i = 0; i<col; i++){
			occ[row-1][i] = 1;
		}
	} 
	/*
	 * 	initialize agency for createDescriptors 
	 */
	private void iniAgency(){
		occ[1][1] = 2;
		occ[1][2] = 2;
	}
	/*
	 * 	initialize destination for createDescriptors
	 */
	private void iniDestination(){
		occ[row-2][col-2] = 2;
		occ[row-2][col-3] = 2;
	}
	/*
	 * 	initialize all the descriptors
	 */
	private void iniDescriptors(){
		short obsLeft = maxObs;	// the number obstructions still left for the PPP
		short lDes;				// the number of obstructions allowed in a single descriptor
		arrayDes = new Descriptor[nDes];
		for (short i = 0; i<nDes; i++){
			Random generator = new Random();
			// the x position of the descriptor
			short rRow = (short)(generator.nextInt(size)+1);
			// the y position of the descriptor
			short rCol = (short)(generator.nextInt(size)*2+1);
			// the type of the descriptor, totally six.
			short type = (short)generator.nextInt(6);
			short currentLength = 0;	// the length of the current descriptor
			/*
			 * 	If the remaining obsLeft is greater than one, then create descriptor
			 * 	If the remaining obsLeft is zero, then create zero length descriptor
			 */
			if(obsLeft>0){
				lDes = lengthDes(obsLeft); // the maximum number of obstructions for one descriptor
				if(occ[rRow][rCol]==0){
					occ[rRow][rCol] = 1;
					occ[rRow][rCol+1] = 1;
					currentLength++;
					lDes--;
				}
				// 0 presents right
				if (type == 0){ 
					for (short j=0; j<lDes; j++){
						if(rCol+j*2<col-2){
							if(occ[rRow][rCol+j*2]==0){
								occ[rRow][rCol+j*2] = 1;
								occ[rRow][rCol+1+j*2] = 1;
								currentLength++;
							} else lDes++;
						}
					}
				}
				// 1 presents left
				if (type == 1){
					for (short j=0; j<lDes; j++){
						if(rCol-j*2>0){
							if(occ[rRow][rCol-j*2]==0){
								occ[rRow][rCol-j*2] = 1;
								occ[rRow][rCol+1-j*2] = 1;
								currentLength++;
							} else lDes++;
						}
					}
				}
				// 2 presents up
				if (type == 2){
					for (short j=0; j<lDes; j++){
						if(rRow-j>0){
							if(occ[rRow-j][rCol]==0){
								occ[rRow-j][rCol] = 1;
								occ[rRow-j][rCol+1] = 1;
								currentLength++;
							} else lDes++;
						}
					}
				}
				// 3 presents down
				if (type == 3){
					for (short j=0; j<lDes; j++){
						if(rRow+j<row-1){
							if(occ[rRow+j][rCol]==0){
								occ[rRow+j][rCol] = 1;
								occ[rRow+j][rCol+1] = 1;
								currentLength++;
							} else lDes++;
						}
					}
				}
				// 4 presents left-up
				if (type == 4){
					for (short j=0; j<lDes; j++){
						if(rCol-j*2>0 && rRow+j<row-1){
							if(occ[rRow+j][rCol-j*2]==0){
								occ[rRow+j][rCol-j*2] = 1;
								occ[rRow+j][rCol+1-j*2] = 1;
								currentLength++;
							} else lDes++;
						}
					}
				}
				// 5 presents left-down
				if (type == 5){
					for (short j=0; j<lDes; j++){
						if(rCol-j*2>0 && rRow-j>0){
							if(occ[rRow-j][rCol-j*2]==0){
								occ[rRow-j][rCol-j*2] = 1;
								occ[rRow-j][rCol+1-j*2] = 1;
								currentLength++;
							} else lDes++;
						}
					}
				}
			}
			arrayDes[i] = new Descriptor(rRow, rCol, currentLength, type);
			obsLeft -= currentLength;
		}
	}
	/*
	 * 	Generate the length of a single descriptor
	 * 	the length of the descriptor is generated by averaging two random numbers
	 */
	private short lengthDes(short obsLeft){
		Random generator = new Random();
		short l = (short)(generator.nextInt(size-1));
		short h = (short)(generator.nextInt(size-1));
		short result = (short)((l+h)/2+1);
		if(result<=obsLeft)
			return result;
		else
			return obsLeft;
	}
	/*
	 * 	Create the array of Descriptors
	 */
	private void createDescriptors(){
		iniOcc();
		iniBoundary();
		iniAgency();
		iniDestination();
		iniDescriptors();
	}
	/*
	 * 	copy the array of Descriptors, given a PPP to create a PPP
	 */
	private void copyDescriptors(){
		iniOcc();
		iniBoundary();
		iniAgency();
		iniDestination();
		updateDescriptors();
	}
	/*
	 * 	return the particular descriptor at given index
	 */
	public Descriptor getDescriptor(int n){
		return arrayDes[n];
	}
	/*
	 * 	return the array of the descriptors for this PPP
	 */
	public Descriptor[] getArrayDes(){
		return arrayDes;
	}
	/*
	 * 	set the descriptor 
	 */
	public void setArrayDes(Descriptor[] des){
		arrayDes = des;
	}
	/*
	 * 	set the descriptor at a particular index
	 */
	public void setDescriptor(Descriptor des, int n){
		arrayDes[n] = des;
	}
	/*
	 * 	Display the following information on the console:
	 * 	the map and the final result of PPP
	 */
	public void displayPPP(){
		displayMap();
		displayFinal();
	}
	/*
	 * 	Print out the array of descriptors on Console
	 */
	public void displayDes(){
		for(short i=0; i<nDes; i++){
			System.out.println(arrayDes[i]);
		}
	}
	/*
	 * 	Display the following information on the console:
	 * 	the map, the space left, the size of the array of the agent state,
	 * 	all the possible state values and the final result of PPP.
	 */
	public void displayPPPwithInfo(){
		displayMap();
		System.out.println("Space left is "+nonOcc());
		System.out.println("The number of obstructions are "+ occ());
		System.out.println("The length of the array is "+ asArray.length);
		displayStateValue();
		displayFinal();
	}
	/*
	 * 	Display the map
	 */
	private void displayMap(){
		for (short i = 0; i<row; i++){
			for (short j = 0; j<col; j++){
				System.out.print(map[i][j]);
			}
			System.out.println();
		}
	}
	/*
	 * 	Get the number of non-occupied cells
	 */
	private short nonOcc(){
		short result = 0;
		for(short i = 0; i<row; i++){
			for(short j = 0; j<col; j++){
				if(occ[i][j]!=1) result++;
			}
		}
		return (short)(result/2);
	}
	/*
	 * 	Get the number of occupied cells
	 */
	private short occ(){
		return (short)(size*size - nonOcc());
	}
	/*
	 * 	Initialize the AgentState Array
	 * 	Output: After the initialization the asArray contains all possible triples
	 * 			that the agent can occupy, and the initial value for each state
	 * 			is (max, max, max) for all possible states except the agent's starting
	 * 			state, set to (0,0,r), which has an initial values of (0,0,0)
	 * 	Tips: j = j + 2 and (j-1)/2 is because two columns represents one space
	 * 		  ((j-1)/2,i-1) is because the array contains boundaries for the map,
	 * 		  and x is the column and y is the row
	 */
	private void iniAgentState(){
		arraySize = (short)(nonOcc()*4);
		short n = 0;
		asArray = new AgentState[arraySize];
		for(short i = 0; i<row; i++){
			for(short j = 1; j<col; j=(short)(j+2)){
				if(occ[i][j]!=1){
					asArray[n*4] = new AgentState((short)((j-1)/2),(short)(i-1),'r');
					asArray[n*4+1] = new AgentState((short)((j-1)/2),(short)(i-1),'u');
					asArray[n*4+2] = new AgentState((short)((j-1)/2),(short)(i-1),'d');
					asArray[n*4+3] = new AgentState((short)((j-1)/2),(short)(i-1),'l');
					n++;
				}
			}
		}
		asArray[0].setStateValue((short)0, (short)0, (short)0);
	}
	/*
	 * Dynamic Programming Algorithm
	 */
	private void dPA(){
		singleDPA();
		AgentState[] temp = new AgentState[arraySize];
		do{
			System.arraycopy(asArray, 0, temp, 0, asArray.length);
			singleDPA();
		} while (!similarArray(asArray, temp));
	}
	/*
	 * 	one loop for the Dynamic Programming Algorithm
	 */
	private void singleDPA(){
		AgentState r, l, a;
		short n;	// the current position of the replacement
		for(short i = 0; i<arraySize; i++){
			r = asArray[i].turnRight();
			n = arrayContain(r);
			if(n!=-1){
				asArray[n] = agentSelector(asArray[n], r);
			}
			l = asArray[i].turnLeft();
			n = arrayContain(l);
			if(n!=-1){
				asArray[n] = agentSelector(asArray[n], l);
			}
			a = asArray[i].advance();
			n = arrayContain(a);
			if(n!=-1){
				asArray[n] = agentSelector(asArray[n], a);
			}
		}
	}
	/*
	 * 	Help function for determine whether the position is a possible position
	 * 	Whether the tested AgentState is in the asArray
	 * 	If the agentState is in the array return the position, if not return -1
	 */
	private short arrayContain(AgentState agentState){
		for(short i = 0; i<arraySize; i++){
			if(asArray[i].similar(agentState)) return i;
		}
		return -1;
	}
	/*
	 * 	Compare the values of two agent state
	 * 	And choose the smaller one
	 */
	private AgentState agentSelector(AgentState a, AgentState b){
		if(a.getStateValue().compareSV(b.getStateValue())) 
			return a;
		else return b;
	}
	/*
	 * 	To check whether the two Arrays are the same in values.
	 * 	Notes: When there is no updates for every state value any more.
	 */
	private boolean similarArray(AgentState[] as1, AgentState[] as2){
		boolean result = true;
		for(short i = 0; i<arraySize; i++){
			result = result && as1[i].getStateValue().sameSV(as2[i].getStateValue());
		}
		return result;
	}
	/*
	 * 	Display the smallest state value of every positions in the asArray
	 * 	Note: different directions in the same position are considered different.
	 */
	private void displayStateValue(){
		for(short i = 0; i<arraySize; i++){
			System.out.print("The position is " + asArray[i] + " and its value is ");
			System.out.println(asArray[i].getStateValue());
		}
	}
	/*
	 * 	Display the final position with direction, 
	 * 	and the smallest state value at destination
	 */
	private void displayFinal(){
		if(availability){
			System.out.println("The final position is " + finalAgentState());
			System.out.println("The smallest state value is " + bestSV);
		} else
			System.out.println("The destination is unreachable!");
	}
	/*
	 * 	check the availability of this PPP
	 */
	private void checkPPP(){
		bestSV = bestStateValue();
		if(bestSV.validSV()){
			availability = true;
		} else {
			availability = false;
		}
	}
	/*	
	 * 	Get the availability of this PPP
	 */
	public boolean checkAvailable(){
		return availability;
	}
	/*
	 * 	Return the state with the direction of facing for the agent to have the 
	 * 	smallest value at the destination
	 */
	private AgentState finalAgentState(){
		finalAS = compareAgentState(asArray[arraySize-1], asArray[arraySize-2],
				asArray[arraySize-3], asArray[arraySize-4]);
		return finalAS;
	}
	/*
	 * 	Return the smallest state value from the start point to the destination
	 */
	private StateValue bestStateValue(){
		return finalAgentState().getStateValue();
	}
	/*
	 * 	Compare four state values and return the smallest one
	 */
	private AgentState compareAgentState(AgentState as1, AgentState as2,
			AgentState as3, AgentState as4){
		AgentState result;
		if(as1.getStateValue().compareSV(as2.getStateValue()))
			result = as1;
		else
			result = as2;
		if(as3.getStateValue().compareSV(result.getStateValue()))
			result = as3;
		if(as4.getStateValue().compareSV(result.getStateValue()))
			result = as4;
		return result;
	}
	/*
	 * 	mutate the arrayDes
	 */
	public void mutatePPP(){
		availability = false;
		tempDes = new Descriptor[nDes];
		System.arraycopy( arrayDes, 0, tempDes, 0, arrayDes.length);
		while(!availability){
			for(int i=0; i<nDes; i++){
				tempDes[i].mutation(size);
			}
			updatePPP();
		}
		System.arraycopy(tempDes, 0, arrayDes, 0, tempDes.length);
	}
	/*
	 * 	Update PPP after mutation and two point crossover
	 */
	public void updatePPP(){
		iniOcc();
		iniBoundary();
		iniAgency();
		iniDestination();
		updateDescriptors();
		iniAgentState();
		dPA();
		checkPPP();
	}
	/*
	 * 	Update the descriptors after mutation
	 */
	private void updateDescriptors(){
		short obsLeft = maxObs;
		short lDes;
		tempDes = new Descriptor[nDes];
		System.arraycopy( arrayDes, 0, tempDes, 0, arrayDes.length);
		for (short i = 0; i<nDes; i++){
			// the x position of the descriptor
			short rRow = tempDes[i].getX();
			// the y position of the descriptor
			short rCol = tempDes[i].getY();
			// the type of the descriptor, totally six.
			short type = tempDes[i].getType();
			short currentLength = 0;	// the length of the current descriptor
			if(obsLeft>0){
				lDes = tempDes[i].getLength(); // the maximum number of obstructions for one descriptor
				if(occ[rRow][rCol]==0){
					occ[rRow][rCol] = 1;
					occ[rRow][rCol+1] = 1;
					currentLength++;
					lDes--;
				}		
				// 0 presents right
				if (type == 0){ 
					for (short j=0; j<lDes; j++){
						if(rCol+j*2<col-2){
							if(occ[rRow][rCol+j*2]==0){
								occ[rRow][rCol+j*2] = 1;
								occ[rRow][rCol+1+j*2] = 1;
								currentLength++;
							} else lDes++;
						}
					}
				}
				// 1 presents left
				if (type == 1){
					for (short j=0; j<lDes; j++){
						if(rCol-j*2>0){
							if(occ[rRow][rCol-j*2]==0){
								occ[rRow][rCol-j*2] = 1;
								occ[rRow][rCol+1-j*2] = 1;
								currentLength++;
							} else lDes++;
						}
					}
				}
				// 2 presents up
				if (type == 2){
					for (short j=0; j<lDes; j++){
						if(rRow-j>0){
							if(occ[rRow-j][rCol]==0){
								occ[rRow-j][rCol] = 1;
								occ[rRow-j][rCol+1] = 1;
								currentLength++;
							} else lDes++;
						}
					}
				}
				// 3 presents down
				if (type == 3){
					for (short j=0; j<lDes; j++){
						if(rRow+j<row-1){
							if(occ[rRow+j][rCol]==0){
								occ[rRow+j][rCol] = 1;
								occ[rRow+j][rCol+1] = 1;
								currentLength++;
							} else lDes++;
						}
					}
				}
				// 4 presents left-up
				if (type == 4){
					for (short j=0; j<lDes; j++){
						if(rCol-j*2>0 && rRow+j<row-1){
							if(occ[rRow+j][rCol-j*2]==0){
								occ[rRow+j][rCol-j*2] = 1;
								occ[rRow+j][rCol+1-j*2] = 1;
								currentLength++;
							} else lDes++;
						}
					}
				}
				// 5 presents left-down
				if (type == 5){
					for (short j=0; j<lDes; j++){
						if(rCol-j*2>0 && rRow-j>0){
							if(occ[rRow-j][rCol-j*2]==0){
								occ[rRow-j][rCol-j*2] = 1;
								occ[rRow-j][rCol+1-j*2] = 1;
								currentLength++;
							} else lDes++;
						}
					}
				}
			}
			tempDes[i].setLength(currentLength);
			obsLeft -= currentLength;
		}
		//iniOcc();
	}
	/*
	 * 	This function is used for creating a valid PPP
	 * 	if the destinatino is unreachable, the PPP will redo everything again
	 * 	to generate a reachable PPP
	 */
	private void iniPPP(){
		while(!availability){
			createDescriptors();
			iniAgentState();
			dPA();
			checkPPP();
		}
	}
	/*
	 * 	This function is used for copy the PPP
	 */
	private void copyPPP(){
		copyDescriptors();
		iniAgentState();
		dPA();
		checkPPP();
	}
	/*
	 * 	Two point crossover
	 */
	public boolean TwoPC(PPP spouse, short p1, short p2){
		tempDes = new Descriptor[nDes];
		System.arraycopy(arrayDes, 0, tempDes, 0, arrayDes.length);
		for(short i=p1; i<=p2; i++){
			tempDes[i] = spouse.getDescriptor(i);
		}
		iniOcc();
		iniBoundary();
		iniAgency();
		iniDestination();
		iniAgentState();
		dPA();
		checkPPP();
		if(availability){
			System.arraycopy(tempDes, 0, arrayDes, 0, tempDes.length);
			return true;
		}else
			return false;
	}
	/*
	 * 	Get the value of turn for this PPP
	 */
	public short getTurn(){
		return bestSV.getTurn();
	}
}
