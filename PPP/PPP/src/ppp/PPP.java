package ppp;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import state.AgentState;
import state.StateValue;
import sim.agent.represenation.Sensor;

/*
 * 	Author: Hao Wei
 * 	Time:	14/05/2013
 * 	Description: PPP(size, lDes, nDes)
 */

public class PPP{
	public short size;	// the size of the PPP
	private short nDes;	// the number of descriptors
	private short maxObs;	// the maximum number of obstructions allowed
	private short row;	// the number of chars in one column
	private short col;	// the number of chars in one row
	private char[][] map;	// the char array of the map
	private Descriptor[] arrayDes;	// the array of the descriptors
	//private Descriptor[] tempDes;	// the temp array of the descriptors, for updatePPP
	/*
	 * 	0 presents the current cell is non-occupied and 1 presents occupied
	 * 	2 presents the cell is occupied by the agent or the destination
	 */
	private short[][] occ; 
	/*
	 * 	The state space array of all possible triples (x,y,h) that the agent can occupy
	 */
	private AgentState[] asArray;
	private short asSize;			// the size of the array of agent state
	private boolean availability;	// the availability of the PPP, whether it is reachable or not
	private StateValue bestSV;		// the best StateValue
	private AgentState finalAS; 	// the final AgentState
	
	//Difficulty
	private int totalCells;
	private int goalVisibleCells;
	private int startVisibleCells;
	private double goalVisiblePercentage;
	private double startVisiblePercentage;
	
	
	/*
	 * 	Initialize the map
	 */
	public PPP(short size, short nDes, short maxObs){
		this.size = size;
		this.nDes = nDes;
		this.maxObs = maxObs;
		this.goalVisibleCells = 0;
		this.totalCells = (size*2)*size;
		row = (short)(size+2);
		col = (short)(size*2+2);
		map = new char[row][col];
		occ = new short[row][col];
		arrayDes = new Descriptor[nDes];
		availability = false;
		createPPP();
	}
	/*
	 * 	Initialize the map using a given PPP
	 */
	public PPP(PPP ppp){
		this.size = ppp.size;
		this.nDes = ppp.nDes;
		this.maxObs = ppp.maxObs;
		this.asSize = ppp.asSize;
		this.goalVisibleCells = 0;
		this.totalCells = (this.size*2)*this.size;
		row = (short)(size+2);
		col = (short)(size*2+2);
		map = new char[row][col];
		occ = new short[row][col];
		arrayDes = new Descriptor[nDes];
		availability = false;
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
	 *  Draw map with the occ
	 */
	public void drawMap(){
		map[1][1] = '-';
		map[1][2] = '>';
		map[row-2][col-2] = '*';
		map[row-2][col-3] = '*';
		for (short i = 0; i<row; i++){
			for (short j = 0; j<col; j++){
				if(occ[i][j]==1){
					map[i][j] = '#';
				}
				if(occ[i][j]==0){
					map[i][j] = '.';
				}
				if(occ[i][j]==3){
					map[i][j] = '[';
				}
				if(occ[i][j]==4){
					map[i][j] = ']';
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
		occ[row-2][col-2] = 10;
		//occ[row-2][col-3] = 10;
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
			short rRow, rCol;
			do{
				// the x position of the descriptor
				rRow = (short)(generator.nextInt(size)+1);
				// the y position of the descriptor
				rCol = (short)(generator.nextInt(size)*2+1);
			}while(occ[rRow][rCol]==2||occ[rRow][rCol]==10);
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
					occ[rRow][rCol] = 3;
					occ[rRow][rCol+1] = 4;
					currentLength++;
					lDes--;
				}
				// 0 presents right
				if (type == 0){ 
					for (short j=0; j<lDes; j++){
						if(rCol+j*2<col-2){
							if(occ[rRow][rCol+j*2]==0){
								occ[rRow][rCol+j*2] = 3;
								occ[rRow][rCol+1+j*2] = 4;
								currentLength++;
							} else lDes++;
						}else{
							break;
						}
					}
				}
				// 1 presents left
				if (type == 1){
					for (short j=0; j<lDes; j++){
						if(rCol-j*2>0){
							if(occ[rRow][rCol-j*2]==0){
								occ[rRow][rCol-j*2] = 3;
								occ[rRow][rCol+1-j*2] = 4;
								currentLength++;
							} else lDes++;
						}else{
							break;
						}
					}
				}
				// 2 presents up
				if (type == 2){
					for (short j=0; j<lDes; j++){
						if(rRow-j>0){
							if(occ[rRow-j][rCol]==0){
								occ[rRow-j][rCol] = 3;
								occ[rRow-j][rCol+1] = 4;
								currentLength++;
							} else lDes++;
						}else{
							break;
						}
					}
				}
				// 3 presents down
				if (type == 3){
					for (short j=0; j<lDes; j++){
						if(rRow+j<row-1){
							if(occ[rRow+j][rCol]==0){
								occ[rRow+j][rCol] = 3;
								occ[rRow+j][rCol+1] = 4;
								currentLength++;
							} else lDes++;
						}else{
							break;
						}
					}
				}
				// 4 presents left-up
				if (type == 4){
					for (short j=0; j<lDes; j++){
						if(rCol-j*2>0 && rRow-j>0){
							if(occ[rRow-j][rCol-j*2]==0){
								occ[rRow-j][rCol-j*2] = 3;
								occ[rRow-j][rCol+1-j*2] = 4;
								currentLength++;
							} else lDes++;
						}else{
							break;
						}
					}
				}
				// 5 presents left-down
				if (type == 5){
					for (short j=0; j<lDes; j++){
						if(rCol-j*2>0 && rRow+j<row-1){
							if(occ[rRow+j][rCol-j*2]==0){
								occ[rRow+j][rCol-j*2] = 3;
								occ[rRow+j][rCol+1-j*2] = 4;
								currentLength++;
							} else lDes++;
						}else{
							break;
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
	private void iniPPP(){
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
		System.out.println("The descriptors are showing below:");
		for(short i=0; i<nDes; i++){
			System.out.print(arrayDes[i]);
			if(i!=(short)(nDes-1)){
				System.out.print(", ");
			}
		}
		System.out.println();
	}
	/*
	 * 	Display the following information on the console:
	 * 	the map, the space left, the size of the array of the agent state,
	 * 	all the possible state values and the final result of PPP.
	 */
	/*public void displayPPPwithInfo(){
		displayMap();
		System.out.println("Space left is "+nonOcc());
		System.out.println("The number of obstructions are "+ occ());
		System.out.println("The length of the array is "+ asArray.length);
		displayStateValue();
		displayFinal();
	}*/
	/*
	 * 	Display the map
	 */
	public void displayMap(){
		for (short i = 0; i<row; i++){
			for (short j = 0; j<col; j++){
				System.out.print(map[i][j]);
			}
			System.out.println();
		}
		System.out.printf("%d x %d squares \n", this.size*2, this.size);
	}
	
	public boolean isOccupied(int x, int y){
		short o = this.occ[y][x];
		switch (o){
		case 1:
		case 3:
		case 4:
			return true;
		default:
			return false;
		}
	}
	
	public short getOccCell(int x, int y){
		return occ[y][x];
	}
	
	public void displayOcc(){
		for (int i = 0; i<row; i++){
			for(int j=0; j<col; j++){
				System.out.print(occ[i][j]);
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
				if(occ[i][j]==0||occ[i][j]==2||occ[i][j]==10) result++;
			}
		}
		return (short)(result/2);
	}
	/*
	 * 	Get the number of occupied cells
	 */
	public short occ(){
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
		asSize = (short)(nonOcc()*4);
		//System.out.println(occ());
		//System.out.println(arraySize);
		short n = 0;
		asArray = new AgentState[asSize];
		//for each column
		for(short i = 0; i<size; i++){
			//for each row
			for(short j = 0; j<size; j++){
				//For each free grid sq. in Occ grid:
				if(occ[j+1][i*2+1]==0||occ[j+1][i*2+1]==2||occ[j+1][i*2+1]==10){
					asArray[n*4] = new AgentState((short)i,(short)j,'r');
					asArray[n*4+1] = new AgentState((short)i,(short)j,'u');
					asArray[n*4+2] = new AgentState((short)i,(short)j,'d');
					asArray[n*4+3] = new AgentState((short)i,(short)j,'l');
					n++;
				}
			}
		}
		//System.out.println(n);
		asArray[0].setStateValue((short)0, (short)0, (short)0);
	}

	/*
	 * 	Help function for determine whether the position is a possible position
	 * 	Whether the tested AgentState is in the asArray
	 * 	If the agentState is in the array return the position, if not return -1
	 */
	private short arrayContain(AgentState agentState){
		for(short i = 0; i<asSize; i++){
			if(asArray[i].similar(agentState)) return i;
		}
		return -1;
	}
	/*
	 * 	Compare the values of two agent state
	 * 	And choose the smaller one
	 */
	private AgentState agentSelector(AgentState a, AgentState b){
		if(a.getStateValue().compareSVT(b.getStateValue())) 
			return a;
		else return b;
	}
	/*
	 * 	To check whether the two Arrays are the same in values.
	 * 	Notes: When there is no updates for every state value any more.
	 */
	private boolean similarAS(AgentState[] as1, AgentState[] as2){
		boolean result = true;
		for(short i = 0; i<asSize; i++){
			result = result && as1[i].getStateValue().sameSV(as2[i].getStateValue());
		}
		return result;
	}
	/*
	 * 	Display the smallest state value of every positions in the asArray
	 * 	Note: different directions in the same position are considered different.
	 */
	/*private void displayStateValue(){
		for(short i = 0; i<arraySize; i++){
			System.out.print("The position is " + asArray[i] + " and its value is ");
			System.out.println(asArray[i].getStateValue());
		}
	}*/
	/*
	 * 	Display the final position with direction, 
	 * 	and the smallest state value at destination
	 */
	public void displayFinal(){
		if(availability){
			System.out.println("The final agent state is " + finalAgentState());
			System.out.println("The smallest state value is " + bestSV);
			System.out.printf("Goal Visible from %d/%d :: Percentage %.2f\n", 
					this.goalVisibleCells, this.totalCells, this.goalVisiblePercentage);
			System.out.printf("Start Visible from %d/%d :: Percentage %.2f\n", 
					this.goalVisibleCells, this.totalCells, this.startVisiblePercentage);
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
		finalAS = compareAgentState(asArray[asSize-1], asArray[asSize-2],
				asArray[asSize-3], asArray[asSize-4]);
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
	public PPP mutatePPP(){
		PPP child = new PPP(this);
		Random generator = new Random();
		boolean reachable = false;
		
		while(!reachable){
			child = new PPP(this);
			int random = generator.nextInt(this.nDes);
			child.arrayDes[random].mutation(this.size);
			child.updatePPP();
			reachable = child.checkAvailable();
		}
		return child;
	}
	/*
	 * 	Update PPP after mutation and two point crossover
	 */
	public void updatePPP(){
		copyDescriptors();
		iniAgentState();
		dPA();
		checkPPP();
		this.evaluateDifficulty();
	}
	/*
	 * 	Update the descriptors after mutation
	 */
	private void updateDescriptors(){
		short obsLeft = maxObs;
		short lDes;
		for (short i = 0; i<nDes; i++){
			// the x position of the descriptor
			short rRow = arrayDes[i].getX();
			// the y position of the descriptor
			short rCol = arrayDes[i].getY();
			// the type of the descriptor, totally six.
			short type = arrayDes[i].getType();
			short currentLength = 0;	// the length of the current descriptor
			if(obsLeft>0){
				lDes = arrayDes[i].getLength(); // the maximum number of obstructions for one descriptor
				if(occ[rRow][rCol]==0){
					occ[rRow][rCol] = 3;
					occ[rRow][rCol+1] = 4;
					currentLength++;
					lDes--;
				}
				// 0 presents right
				if (type == 0){ 
					for (short j=0; j<lDes; j++){
						if(rCol+j*2<col-2){
							if(occ[rRow][rCol+j*2]==0){
								occ[rRow][rCol+j*2] = 3;
								occ[rRow][rCol+1+j*2] = 4;
								currentLength++;
							} else lDes++;
						}else{
							break;
						}
					}
				}
				// 1 presents left
				if (type == 1){
					for (short j=0; j<lDes; j++){
						if(rCol-j*2>0){
							if(occ[rRow][rCol-j*2]==0){
								occ[rRow][rCol-j*2] = 3;
								occ[rRow][rCol+1-j*2] = 4;
								currentLength++;
							} else lDes++;
						}else{
							break;
						}
					}
				}
				// 2 presents up
				if (type == 2){
					for (short j=0; j<lDes; j++){
						if(rRow-j>0){
							if(occ[rRow-j][rCol]==0){
								occ[rRow-j][rCol] = 3;
								occ[rRow-j][rCol+1] = 4;
								currentLength++;
							} else lDes++;
						}else{
							break;
						}
					}
				}
				// 3 presents down
				if (type == 3){
					for (short j=0; j<lDes; j++){
						if(rRow+j<row-1){
							if(occ[rRow+j][rCol]==0){
								occ[rRow+j][rCol] = 3;
								occ[rRow+j][rCol+1] = 4;
								currentLength++;
							} else lDes++;
						}else{
							break;
						}
					}
				}
				// 4 presents left-up
				if (type == 4){
					for (short j=0; j<lDes; j++){
						if(rCol-j*2>0 && rRow-j>0){
							if(occ[rRow-j][rCol-j*2]==0){
								occ[rRow-j][rCol-j*2] = 3;
								occ[rRow-j][rCol+1-j*2] = 4;
								currentLength++;
							} else lDes++;
						}else{
							break;
						}
					}
				}
				// 5 presents left-down
				if (type == 5){
					for (short j=0; j<lDes; j++){
						if(rCol-j*2>0 && rRow+j<row-1){
							if(occ[rRow+j][rCol-j*2]==0){
								occ[rRow+j][rCol-j*2] = 3;
								occ[rRow+j][rCol+1-j*2] = 4;
								currentLength++;
							} else lDes++;
						}else{
							break;
						}
					}
				}
			}
			arrayDes[i].setLength(currentLength);
			obsLeft -= currentLength;
		}
	}
	/*
	 * 	This function is used for creating a valid PPP
	 * 	if the destinatino is unreachable, the PPP will redo everything again
	 * 	to generate a reachable PPP
	 */
	private void createPPP(){
		while (!this.availability){
			iniPPP();
			iniAgentState();
			dPA();
			checkPPP();
		}
		evaluateDifficulty();
	}
	/*
	 * 	This function is used for copy the PPP
	 */
	private void copyPPP(){
		copyDescriptors();
		iniAgentState();
		dPA();
		checkPPP();
		evaluateDifficulty();
	}
	/*
	 * 	Get the value of turn for this PPP
	 */
	public short getTurn(){
		if(!bestSV.validSV()){
			return (short) 0;
		}
		return bestSV.getTurn();
	}
	/*
	 * 	Get the value of advance for this PPP
	 */
	public short getAdvance(){
		return bestSV.getAdvance();
	}
	/*
	 * 	Get the value of move for this PPP
	 */
	public short getMove(){
		return bestSV.getMove();
	}
	/*
	 * 	check whether two PPPs are same in value or not
	 */
	public boolean samePPP(PPP target){
		boolean result = true;
		for (short i = 0; i<row; i++){
			for (short j = 0; j<col; j++){
				if(occ[i][j]!=target.occ[i][j]){
					result = false;
				}
			}
		}
		return result;
	}
	
	/*
	 * 	write the PPP to a file, named PPP
	 */
	public void writePPP(int number){
		this.writePPP("", number);
	}
	
	public void writePPP (String folder, int number) {
		Writer writer = null;

		try {
		    writer = new BufferedWriter(new OutputStreamWriter(
		          new FileOutputStream(folder+"/PPP"+number+".ppp"), "utf-8"));
		    writer.write("PPP\n");
		    writer.write("#.\n");
		    writer.write((size+2)+" "+(size+2)+" "+(maxObs)+ "\n");
		    writer.write(bestStateValue().getMove()+"\n");
		    
		    for(short i=0; i<nDes; i++){
				writer.write(arrayDes[i].write());
				if(i!=(short)(nDes-1)){
					writer.write(", ");
				}
			}
		    writer.write("\n");
		    
		    for (short i = 0; i<row; i++){
				for (short j = 0; j<col; j++){
					// Boundary wall
					if(occ[i][j]==1){
						writer.write("2 ");
						if(j!=0&j!=col-1){
							j++;
						}
					}
					//Empty space or goal / initial position
					if(occ[i][j]==0||occ[i][j]==2||occ[i][j]==10){
						writer.write("0 ");
						j++;
					}
					// Left side of obstacle
					if(occ[i][j]==3){
						writer.write("1 ");
						j++;
					}
				}
				writer.write("\n");
			}
		} catch (IOException ex){
			ex.printStackTrace();
			System.exit(1);
		} finally {
		   try {writer.close();} catch (Exception ex) {}
		}
	}
	
	public short[][] getOccGrid(){
		return this.occ;
	}
	
	/**
	 * Evaluation
	 */
	
	/*
	 * Dynamic Programming Algorithm
	 */
	private void dPA(){
		singleDPA();
		AgentState[] temp = new AgentState[asSize];
		do{
			System.arraycopy(asArray, 0, temp, 0, asArray.length);
			singleDPA();
		} while (!similarAS(asArray, temp));
	}
	
	/*
	 * 	one loop for the Dynamic Programming Algorithm
	 */
	private void singleDPA(){
		AgentState r, l, a;	// stands for right, left, advance
		short n;			// the current position of the replacement
		for(short i = 0; i<asSize; i++){
			AgentState temp = new AgentState(asArray[i]);
			r = temp.turnRight();
			n = arrayContain(r);
			if(n!=-1){
				asArray[n] = agentSelector(asArray[n], r);
			}
			temp = new AgentState(asArray[i]);
			l = temp.turnLeft();
			n = arrayContain(l);
			if(n!=-1){
				asArray[n] = agentSelector(asArray[n], l);
			}
			temp = new AgentState(asArray[i]);
			a = temp.advance();
			n = arrayContain(a);
			if(n!=-1){
				asArray[n] = agentSelector(asArray[n], a);
			}
		}
	}
	
	public void evaluateDifficulty(){
		this.goalVisibleCells = this.evaluateGoalVisibility();
		this.goalVisiblePercentage = this.goalVisibleCells / (double)this.totalCells;
		
		this.startVisibleCells = this.evaluateStartPosVisibility();
		this.startVisiblePercentage = this.startVisibleCells / (double)this.totalCells;
	}
	
	/**
	 * Evaluate the visibility of the goal position assuming a perfect sensor
	 * I.e. no noise and with a range covering the map
	 * We calculate this by looking out from the position specified;
	 * Cells visible from the pos have visibility onto that pos .
	 * This is less intensive than checking visibilty for every possible position.
	 * @return count of cells from which the pos is visible
	 */
	private int evaluateVisibilityFromPosition(int xPos, int yPos, boolean showOnMap){
		//calculate the number of cells from which the goal position is visible
		int range = (this.size*2)+2;
		Sensor sensor = new Sensor(range);
		int[] goalPos = new int[] {col-2, row-2};
		int[] senseWindow = sensor.boundSenseWindow(xPos, yPos, this.size);
		int x_left   = senseWindow[0];
		int x_right  = senseWindow[1];
		int y_top    = senseWindow[2];
		int y_bottom = senseWindow[3];
		
		HashSet<List<Short>> visibleCells = new HashSet<List<Short>>();
		
		for (int y=y_top; y<=y_bottom; y++){
			int[] endPoints = Sensor.getEndPoints(x_left, x_right);
			for (int x: endPoints){
				//Line from goalPos to cell (endX, Y)
				//Scan along this line to determine number of cells which can see the goal
				ArrayList<short[]> LoS = Sensor.line(xPos, yPos, x, y);
				for (short[] p: LoS){
					if(this.isOccupied(p[0], p[1])){
						//Can't sense from, or from behind, an occupied cell.
						//The rest of the line has it's visibilty blocked by this cell,
						//so skip to next LoS
						break;
					} else {
						visibleCells.add(Arrays.asList(p[0], p[1]));
						if (showOnMap){
							map[p[1]][p[0]] = 'v';
						}
					}
				}
			}
		}
		return visibleCells.size();
	}
	
	private int evaluateGoalVisibility(){
		return this.evaluateVisibilityFromPosition(col-2, row-2, false);
	}
	private int evaluateStartPosVisibility(){
		return this.evaluateVisibilityFromPosition(1, 1, true);
	}
	
	public double getVisibilityPercentage(){
		return this.goalVisiblePercentage;
	}
	
	public int getVisibilityCount(){
		return this.goalVisibleCells;
	}
	
	
}
