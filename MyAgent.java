package wumpusworld;


import java.util.ArrayList;

/**
 * Contains starting code for creating your own Wumpus World agent.
 * Currently the agent only make a random decision each turn.
 * 
 * @author Johan Hagelbäck
 */
public class MyAgent implements Agent
{
  
    //Variables-----------------------------------------------------------------
    private World w;
    int rnd;
    private Cell[][] board;
    boolean wumpusFound = false;
    
    //"Rules"-------------------------------------------------------------------    
    private boolean isVisited(Cell c) {
        return c.travel == Cell.VISITED;
    }
    
    private boolean isAccessible(Cell c) {
        return c.travel == Cell.ACCESSIBLE;
    }
    
    private boolean isRiskless(Cell c) {
        int b = c.numBreezes;//breezes
        int s = c.numStenches;//stenches
        int v = c.numVisited;//visited neibours
        
        boolean isRisky = (b < v    &&  (s < v  ||  wumpusFound));
        return isRisky;
        //If we can get higher number of visited neibours for each cell,it is easy to determine where the wumpus or 
        // pit is. if we have less number of breezes or stenches around a cell we can conclude that it is bit safe.
        //If we could find the wumpus we can always shoot the wumpus and can disregard stenches.
    }
    
    private boolean isPathSafe(Path p) {
        return ( isRiskless( p.cellList.get(0) ) && (p.cost < 1000) );
    }
    
    private int RiskLevel(Cell c) {
        
        int riskLevel=c.numNeighbours - c.numVisited;
        return riskLevel;
        //This will calculate the risk level for each cell.
        //If we have visited more neibours the risk level will be low.
        //The more we know about the neibours its easier to determine the risk for each cell
    }
    
    //Functions-----------------------------------------------------------------
    /**
     * Creates a new instance of your solver agent.
     * 
     * @param world Current world state 
     */
    public MyAgent(World world)
    {
        System.out.println("\nCREATING NEW AGENT\n");
        w = world;
        
        //Create the board and set the number of neighbours of each cell
        board = new Cell[4][4];
        
        int nN;
        for(int x=0;x<4;x++) {
            for(int y=0;y<4;y++) {
                nN = 4;//Reset to 4 neighbours
                if(x == 0 || x == 3) {  
                    nN--;   
                }   
                if(y == 0 || y == 3) {  
                    nN--;   
                }   
    //Reduce number of neighbours by one for each wall the cell is adjecent to
    
                board[x][y] = new Cell(x, y, nN);
            }
        }
    }
            
    /**
     * Asks your solver agent to execute an action.
     */
    public void doAction()
    {
        System.out.println("In doAction()");
        
        //Location of the player
        int cX = w.getPlayerX();
        int cY = w.getPlayerY();
        
        //Check and update if we entered a new space
        if(newSpace(cX, cY)) {
            System.out.println("New Space added: (" + (cX-1) + "," + (cY-1) + ")");
        }
        
        //Pick up gold if we have found it, then exit early
        if(w.hasGlitter(cX, cY)) {
            System.out.println("Picked the gold!");
            w.doAction(World.A_GRAB);
            return;
        }
        
        //Get all accessible cells
        ArrayList<Cell> acCells = getAccessibleCells();
        outputCellList(acCells,"Accessible Cells");
        
        //Search for wumpus
        wumpusFound = findWumpus(acCells);
        
        //Build paths to all the cells
        ArrayList<Path> acPaths = pathsToCells(acCells);
        outputPathList(acPaths,"Accessible Paths");
        
        //Select the best choice(safest and cheapest path)
        Path bestPath = safestPath(acPaths);
        outputPath(bestPath,"Selected Path");
        
        //Make a move
        followPath(bestPath);
        System.out.println("\n");
        
    }    
    
    public boolean newSpace(int x, int y) {
        
        //If we have added new space it updates the Cell of the space we entered and its surrounding spaces' Cells
        
        //x and y are 1-4, array should be 0-3
        int ax = x-1;
        int ay = y-1;
        
        //If already visited
        if(isVisited(board[ax][ay])) {    
            return false;   
        }
        
        //TRAVEL BOARD CALCULATION
        //Set cell we traveled into as VISITED
        board[ax][ay].travel = Cell.VISITED;
        
        //Set neighbour cells that are not out of scope AND not visted to accessible
        if(ay+1 < 4  &&  board[ax][ay+1].travel != Cell.VISITED){   
            board[ax][ay+1].travel = Cell.ACCESSIBLE;   
        }
        if(ax-1 > -1 &&  board[ax-1][ay].travel != Cell.VISITED){
            board[ax-1][ay].travel = Cell.ACCESSIBLE;   
        }
        if(ax+1 < 4  &&  board[ax+1][ay].travel != Cell.VISITED){
            board[ax+1][ay].travel = Cell.ACCESSIBLE;   
        }
        if(ay-1 > -1 &&  board[ax][ay-1].travel != Cell.VISITED){
            board[ax][ay-1].travel = Cell.ACCESSIBLE;   
        }
        
        //PERCEPT BOARD CALCULATION
        //If we find a percept in new space we can log nearby cells saying that those might cause that percepts
        
        //Set newly found breezes
        if(w.hasBreeze(x, y)) {
            if(ay+1 < 4){   board[ax][ay+1].numBreezes++;   }
            if(ax-1 > -1){  board[ax-1][ay].numBreezes++;   }
            if(ax+1 < 4){   board[ax+1][ay].numBreezes++;   }
            if(ay-1 > -1){  board[ax][ay-1].numBreezes++;   }
        }
        
        //Set newly found stenches
        if(w.hasStench(x, y)) {
            if(ay+1 < 4){   board[ax][ay+1].numStenches++;   }
            if(ax-1 > -1){  board[ax-1][ay].numStenches++;   }
            if(ax+1 < 4){   board[ax+1][ay].numStenches++;   }
            if(ay-1 > -1){  board[ax][ay-1].numStenches++;   }
        }
        
        //Set guaranteed new visited neighbour
        if(ay+1 < 4){   board[ax][ay+1].numVisited++;   }
        if(ax-1 > -1){  board[ax-1][ay].numVisited++;   }
        if(ax+1 < 4){   board[ax+1][ay].numVisited++;   }
        if(ay-1 > -1){  board[ax][ay-1].numVisited++;   }
        
        //OBJECT BOARD CALCULATION
        //The gold percept can be resolved outside since we win
        //and no need to enter wumpus beacuse we are dead
        board[ax][ay].pit = w.isInPit();

        return true;
    }
    
    private ArrayList<Cell> getAccessibleCells() {
        //makes a list with accessible cells
        ArrayList<Cell> accList = new ArrayList<>();
        
        for(int x=0;x<4;x++) {
            for(int y=0;y<4;y++) {
                if(isAccessible(board[x][y])) {                //Check if the cell is accessible
                    accList.add(board[x][y]);                  //if so add it to list
                }
            }
        }
        
        return accList;
    }
    
    private boolean findWumpus(ArrayList<Cell> cList) {
        //Fist we determine if the wumpus already has been found, and if so return early
        if(wumpusFound) { 
            return true;    
        }
       
        //If wumpus not found we can count the stenches in all accessible cells.
        //The cell with the most number of stenches will have the wumpus
        
        int wumpusIndex = -1;
        int mostStenches = 0;
        
        for(int i=0;i<cList.size();i++) {
            
            //if we find a cell with most stenches we replace current most index with the new index.
            //if no of stenches are equal we will reset the index to -1 beacuse not sure where the wumpus is
            
            if(mostStenches < cList.get(i).numStenches) {
                mostStenches = cList.get(i).numStenches;
                wumpusIndex = i;
            }
            else if(mostStenches == cList.get(i).numStenches) {
                wumpusIndex = -1;
            }
        }
        
        //After looping through we now see if any of out accessible cells is known
        //to have the wumpus.
        //If not, return false. 
        //Otherwise set that cell to hold the wumpus and tell the agent we have found the wumpus
        
        if(wumpusIndex == -1) {
            return false;   
        }
        
        cList.get(wumpusIndex).wumpus = true;
        return true;
        
    }
    
    private ArrayList<Path> pathsToCells(ArrayList<Cell> cList) {
        
        ArrayList<Path> pathList = new ArrayList<>();
        
        ArrayList<Cell> cellList = new ArrayList<>();
        
        int ax = w.getPlayerX() - 1;
        int ay = w.getPlayerY() - 1;
        Cell currentCell = board[ax][ay];
        
        for(int i=0;i<cList.size();i++) {
            pathList.add(    bestPathTo(cList.get(i), cellList, currentCell)    );
        }
        
        return pathList;
    }
    
    private Path bestPathTo(Cell target, ArrayList<Cell> cellList, Cell currentCell) {

        Path retPath = new Path();
        
        //Calculate the cost for entering this space
        int cellCost = 0;
        cellCost++;                              
        if(currentCell.pit) {       //if fall into a pit cost is 1000 and to climb up 1             
            cellCost += 1001;                       
        }
             
        //Create a list describing the path we have traveled, including this node
        ArrayList<Cell> pathSoFar = (ArrayList<Cell>) cellList.clone();
        pathSoFar.add(currentCell);
        
        //Check for accessible neighbours
        ArrayList<Cell> nbList = getNeighbours(currentCell, Cell.ACCESSIBLE);
        
        //Check if the target is among them
        if(nbList.contains(target)) {
            
            //Add the goal to the path
            retPath.cellList.add(target);
            //Add this cell to the path
            retPath.cellList.add(currentCell);
            
            //Calculate cost
            retPath.cost++;                                     //Movement
            if(target.wumpus) {    
                retPath.cost += 11; //for shooting wumpus
            }
            
            retPath.cost += cellCost;                           

            return retPath;
            
        }
        
        //Check for visited neighbours
        nbList.clear();
        nbList = getNeighbours(currentCell, Cell.VISITED);
        
        //Remove neighbours we have traveled through earlier
        
        for(int i=0;i<nbList.size();i++) {
            if(cellList.contains(nbList.get(i))) {      //If the cell list already contains that neighbour
                nbList.remove(i);                         //Remove cell from neighbour list
                i--;        //Just to ensure we do not accidentally skip an element
            }
        }
        
        //Return if there are no valid neighbours
        if(nbList.isEmpty()) {
            
            //Return retPath with a cost = 1 000 000
            retPath.cost = 1000000;
            return retPath;
        }
        
        
        //Recurse over the valid neighbours and recieve the paths we need
        Path pathNew;
        retPath.cost = 1000000;
        
        for(int i=0;i<nbList.size();i++) {
            
            pathNew = bestPathTo(target, pathSoFar, nbList.get(i));
            
            //Selct the path with the lowest cost
            if(pathNew.cost < retPath.cost) {
                retPath = pathNew;
            }
            
        }
        
        //Now edit retPath values with those of the current cell
        retPath.cellList.add(currentCell);          //Add the current cell to the path
        retPath.cost += cellCost;                   //Add the cost of the current node
   
        return retPath;
    
    }

    private ArrayList<Cell> getNeighbours(Cell c, int travelStatus) {
        ArrayList<Cell> nbList = new ArrayList<>();
        
        int cx = c.px;
        int cy = c.py;
            
        //If a cell is inside the scope and already visited we add it to the neighbour list
        if(cy+1 < 4 &&  board[cx][cy+1].travel == travelStatus) {   nbList.add(board[cx][cy+1]);  }
        if(cx-1 > -1 &&  board[cx-1][cy].travel == travelStatus) {   nbList.add(board[cx-1][cy]);  }
        if(cx+1 < 4 &&  board[cx+1][cy].travel == travelStatus) {   nbList.add(board[cx+1][cy]);  }
        if(cy-1 > -1 &&  board[cx][cy-1].travel == travelStatus) {   nbList.add(board[cx][cy-1]);  }
        
        return nbList;
    }
    
    private Path safestPath(ArrayList<Path> pList) {
        
        
        //Determine if there are any paths that are guaranteed to be safe
        ArrayList<Path> safeList = SafePath(pList);
        System.out.println("Number of Safe Paths(100%): " + safeList.size());
        
        //If we have any 100% safe paths we return them
        if(!safeList.isEmpty()) {   
            return cheapestPath(safeList);    
        }
        
        //Get all paths that have a notion of danger
        ArrayList<Path> riskList = riskyPaths(pList);
        if(riskList.isEmpty()) {    
            System.out.println("ERROR: No Cells in safeList\n");    
        }
        
        return cheapestPath(riskList);
    }
    
    private ArrayList<Path> SafePath(ArrayList<Path> pList) {
                
        int nA = pList.size();
        ArrayList<Path> safeList = new ArrayList<>();
        
        for(int i=0;i<nA;i++) {
            if( isPathSafe(pList.get(i)) ) {
                safeList.add(pList.get(i));
            }
        }
        
        return safeList;
    }
    
    private ArrayList<Path> riskyPaths(ArrayList<Path> pList) {
        //This function returns a list of the  paths to the safest cells in our grid
        
        int nA = pList.size();
        ArrayList<Path> retList = new ArrayList<>();
        
        int lowestRiskLevel = 100;
        ArrayList<Integer> riskLevelList = new ArrayList<>();
        Cell goalCell;
        
        for(int i=0;i<nA;i++){
            goalCell = pList.get(i).cellList.get(0);
            
            riskLevelList.add(riskOfEnteringDenominator(goalCell));
            
            riskLevelList.add(lowestRiskLevel);                                                           //Add denominator for cellList(i) to list
        }
        
        //Go through riskLevelList and find the biggest denominator(s) as those would indicate the
        //safest spaces to enter
        int highestRiskLevel = -100;
        for(int i=0;i<nA;i++) {
            if(highestRiskLevel == riskLevelList.get(i)) {          //If we find an equal denominator we add its cell to the safe list
                retList.add(pList.get(i));
            }
            else if (highestRiskLevel < riskLevelList.get(i)) {    //If we find a better alternative we clear the list and add the better cell
                highestRiskLevel = riskLevelList.get(i);
                retList.clear();
                retList.add(pList.get(i));
            }
        }
        
        return retList;
    }
    
    private Path cheapestPath(ArrayList<Path> pList) {
        
        int bestI = -1;
        int bestCost = 1000000;
        
        for(int i=0;i<pList.size();i++) {
            if(bestCost > pList.get(i).cost) {
                bestCost = pList.get(i).cost;
                bestI = i;
            }
        }
        
        return pList.get(bestI);
    }
    
    private ArrayList<Cell> selectSafestCells(ArrayList<Cell> cList) {
        
        int nA = cList.size();
        ArrayList<Cell> retList = new ArrayList<>();
        
        for(int i=0;i<nA;i++) {            
            if( isRiskless(cList.get(i)) ) {
                retList.add(cList.get(i));
            }
        }
        
        return retList;
    }
    
    private ArrayList<Cell> leastRiskyCells(ArrayList<Cell> cList) {
        //This function return a list of the least risky cells in our grid
        
        int nA = cList.size();
        ArrayList<Cell> retList = new ArrayList<>();
        
        int lowestRiskLevel = 100;
        
        ArrayList<Integer> riskLevelList = new ArrayList<>();
        
        for(int i=0;i<nA;i++){
            riskLevelList.add(riskOfEnteringDenominator(cList.get(i)));
            riskLevelList.add(lowestRiskLevel);
        }
        
        //Go through riskLevelList and find the highest risk as those would indicate the
        //safest spaces to enter
        int highestRiskLevel = -100;
        for(int i=0;i<nA;i++) {
            if(highestRiskLevel == riskLevelList.get(i)) {          //If we find an equal denominator we add its cell to the safe list
                retList.add(cList.get(i));
            }
            else if (highestRiskLevel < riskLevelList.get(i)) {    //If we find a better value we clear the list and add the better cell
                highestRiskLevel = riskLevelList.get(i);
                retList.clear();
                retList.add(cList.get(i));
            }
        }
        
        return retList;
    }
    
    private int riskOfEnteringDenominator(Cell c) {
        //For each Acceessible space A a we now select all Visited spaces S (as these are the ones we would enter from)
        //and calculate the risk. The highest risk from any S is the overall risk of entering A

        //Cell's position
        int cx = c.px;
        int cy = c.py;  
        int riskValue;
        int lowestRiskLevel = 100;
            
        System.out.println("------- Cell [" + cx + "," + cy + "] Calculation -------");
        
        ArrayList<Cell> nbList = getNeighbours(c, Cell.VISITED);
            
        //to find the lowest denominator (highest risk)
        for(int d=0;d<nbList.size();d++) {
            riskValue = RiskLevel(nbList.get(d));       //Calculate the risk
                
            System.out.println("Risk value [" + d + "]: " + riskValue);
                
            if(riskValue < lowestRiskLevel) {   
                lowestRiskLevel = riskValue;    
            } //Save it if it is the lowest found
                
        }
        
        System.out.println("Final Risk Value: " + lowestRiskLevel);
        
        return lowestRiskLevel;
    }
    
    private void followPath(Path p) {
        
        int pLen = p.cellList.size();
        
        for(int i=(pLen-1);i>0;i--) {
            //Turn ourselves to face the right direction
            turnToFace(p.cellList.get(i), p.cellList.get(i-1));

            //Register and climb up from any pit we fall into
            if(w.isInPit()) {
                System.out.println("In Pit, node[" + i + "]");
                
                if(i-1 >= 0) {  p.cellList.get(i-1).setPit(); }
                w.doAction(World.A_CLIMB);
            }
            
            //Check if last step bring us to wumpus
            //If so, shoot it
            if(i == 1   &&  p.cellList.get(i-1).wumpus){
                System.out.println("Shooting the Arrow..");
                w.doAction(World.A_SHOOT);
                System.out.println("Wumpus Killed!");
            }
            
            //Then move
            w.doAction(World.A_MOVE);            
        }
        
    }
    
    private void turnToFace(Cell a, Cell b) {
        
        int cellDirX = b.px - a.px;
        int cellDirY = b.py - a.py;
        
        if(cellDirY == 1) { turnUp();   }
        else if(cellDirY == -1) { turnDown();   }
        
        if(cellDirX == -1) { turnLeft();   }
        else if(cellDirX == 1) { turnRight();   }
        
    }
    
    private void turnUp() {
        while(w.getDirection() != World.DIR_UP){
            if(w.getDirection() == World.DIR_RIGHT){
                w.doAction(World.A_TURN_LEFT);
            }
            else{
                w.doAction(World.A_TURN_RIGHT);
            }
        }
    }
    
    private void turnLeft() {
        while(w.getDirection() != World.DIR_LEFT){
            if(w.getDirection() == World.DIR_UP){
                w.doAction(World.A_TURN_LEFT);
            }
            else{
                w.doAction(World.A_TURN_RIGHT);
            }
        }
    }
    
    private void turnRight() {
        while(w.getDirection() != World.DIR_RIGHT){
            if(w.getDirection() == World.DIR_DOWN){
                w.doAction(World.A_TURN_LEFT);
            }
            else{
                w.doAction(World.A_TURN_RIGHT);
            }
        }
    }
    
    private void turnDown() {
        while(w.getDirection() != World.DIR_DOWN){
            if(w.getDirection() == World.DIR_LEFT){
                w.doAction(World.A_TURN_LEFT);
            }
            else{
                w.doAction(World.A_TURN_RIGHT);
            }
        }
    }
    
    //Output Functions----------------------------------------------------------
    private void outputCellList( ArrayList<Cell> cList,String t) {
        int cx;
        int cy;
        boolean wum;
        boolean pit;
        System.out.println(t + ": size(" + cList.size() + ")");
        for(int i=0;i<cList.size();i++) {
            cx = cList.get(i).px;
            cy = cList.get(i).py;
            wum = cList.get(i).wumpus;
            pit = cList.get(i).pit;
            System.out.println("        [" + i + "]: (" + cx + "," + cy + ") Wumpus: " + wum + " Pit: " + pit);
        }
    }
    
    private void outputPathList(ArrayList<Path> pList,String s) {

        for(int i=0;i<pList.size();i++) {
            outputPath(pList.get(i),(s + "[" + i + "]"));
        }
        
    }
    
    private void outputPath(Path p,String s) {
        System.out.println( s + ": Cost(" + p.cost + ") |"); 
        outputCellList(p.cellList,"    Cells");
    }
}

class Cell {
        //DEFAULTED: Tells us if the default constructor was used or not
        public boolean defaulted = true;
        
        //POS: A cell's position on the grid. default values -1
        public int px = -1;
        public int py = -1;

        //TRAVEL: Where we have been and where we can reach
        public static final int VISITED = 2;        //Visited cells are cells we have gone to
        public static final int ACCESSIBLE = 1;     //Accessible cells are adjecent to visited cells
        public static final int UNDISCOVERED = 0;   //The remaining cells
        public int travel = 0;
        
        //NEAR PERCEPTS: How many of the adjecent spaces that display certain properties
        public int numBreezes = 0;
        public int numStenches = 0;
        public int numVisited = 0;
        public int numNeighbours = 0;
        
        //OBJECTS: What a cell contains
        public boolean wumpus = false;
        public boolean pit = false;
        public boolean gold = false;
        
        //CONSTRUCTORS
        public Cell() {
            defaulted = true;
        }
        
        public Cell(int x, int y, int nN) {
            defaulted = false;
            px = x;
            py = y;
            numNeighbours = nN;
        }
        
        public void setPit() {
            pit = true;
        }
    }
    
    class Path {
        //public Cell target;
        public ArrayList<Cell> cellList;
        public int cost = 0;
        
        public Path() {
            cellList = new ArrayList<>();
        }
    }