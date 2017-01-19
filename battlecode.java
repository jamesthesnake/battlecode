package secondplayer;
import battlecode.common.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
public strictfp class RobotPlayer {
  static RobotController rc;
   float health=50;
   /**
    * run() is the method that is called when a robot is instantiated in the Battlecode world.
    * If this method returns, the robot dies!
   **/
   @SuppressWarnings("unused")
   public static void run(RobotController rc) throws GameActionException {
     
       // This is the RobotController object. You use it to perform actions from this robot,
       // and to get information on its current status.
       RobotPlayer.rc = rc;

       // Here, we've separated the controls into a different method for each RobotType.
       // You can add the missing ones or rewrite this into your own control structure.
       switch (rc.getType()) {
           case ARCHON:
               runArchon();
               break;
           case GARDENER:
               runGardener();
               break;
       }
  }
   static void runArchon() throws GameActionException {
       System.out.println("I'm an archon!");

       // The code you want your robot to perform every round should be in this loop
       while (true) {

           // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
           try {

               // Generate a random direction
               Direction dir = randomDirection();

               // Randomly attempt to build a gardener in this direction
               if (rc.canHireGardener(dir) && Math.random() < .01) {
                   rc.hireGardener(dir);
               }
               if (rc.getTeamBullets()>=10000){
                   rc.donate(10000);
               }

               // Move randomly
               tryMove(randomDirection());

               // Broadcast archon's location for other robots on the team to know
               MapLocation myLocation = rc.getLocation();
               rc.broadcast(0,(int)myLocation.x);
               rc.broadcast(1,(int)myLocation.y);

               // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
               Clock.yield();

           } catch (Exception e) {
               System.out.println("Archon Exception");
               e.printStackTrace();
           }
       }
   }
   static void runGardener() throws GameActionException {
       System.out.println("I'm a gardener!");
       int moving=0;

       // The code you want your robot to perform every round should be in this loop
       while (true){

           // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
           try {
               // Generate a random direction
               if (rc.canPlantTree(Direction.getEast())&&rc.canPlantTree(Direction.getWest())&&rc.canPlantTree(Direction.getSouth())&&rc.canPlantTree(Direction.getNorth())){
                   moving=5;
               }
               if (moving==0&&rc.senseNearbyTrees(1).length==0)
               {
                   tryMove(randomDirection());
               }
               if(moving==5) {


                   if (rc.canPlantTree(Direction.getEast())) {
                       rc.plantTree(Direction.EAST);
                   }
                   if (rc.canPlantTree(Direction.getWest())) {
                       rc.plantTree(Direction.WEST);
                   }
                   if (rc.canPlantTree(Direction.getSouth())) {
                       rc.plantTree(Direction.SOUTH);
                   }
                   if (rc.canPlantTree(Direction.getNorth())) {
                       rc.plantTree(Direction.NORTH);
                   }

               }
               watering();

               // Listen for home archon's location
               int xPos = rc.readBroadcast(0);
               int yPos = rc.readBroadcast(1);
               MapLocation archonLoc = new MapLocation(xPos,yPos);

               // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
               Clock.yield();
           } catch (Exception e) {
               System.out.println("Gardener Exception");
               e.printStackTrace();
           }
          
       }
   }
   public static void watering() throws GameActionException{
       TreeInfo[] closetrees=rc.senseNearbyTrees();
       for (int i = 0; i <closetrees.length ; i++) {
           if(closetrees[i].getHealth()<GameConstants.BULLET_TREE_MAX_HEALTH-GameConstants.WATER_HEALTH_REGEN_RATE){
               if (rc.canWater(closetrees[i].getID())){

                   rc.water(closetrees[i].getID());
                   break;
               }
           }

       }
   }
   static Direction randomDirection() {
       return new Direction((float)Math.random() * 2 * (float)Math.PI);
   }
  

   /**
    * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
    *
    * @param dir The intended direction of movement
    * @return true if a move was performed
    * @throws GameActionException
    */
   static boolean tryMove(Direction dir) throws GameActionException {
       return tryMove(dir,20,3);
   }

   /**
    * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
    *
    * @param dir The intended direction of movement
    * @param degreeOffset Spacing between checked directions (degrees)
    * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
    * @return true if a move was performed
    * @throws GameActionException
    */
   static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

       // First, try intended direction
       if (rc.canMove(dir)) {
           rc.move(dir);
           return true;
       }

       // Now try a bunch of similar angles
       boolean moved = false;
       int currentCheck = 1;

       while(currentCheck<=checksPerSide) {
           // Try the offset of the left side
           if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
               rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
               return true;
           }
           // Try the offset on the right side
           if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
               rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
               return true;
           }
           // No move performed, try slightly further
           currentCheck++;
       }

       // A move never happened, so return false.
       return false;
   }

   /**
    * A slightly more complicated example function, this returns true if the given bullet is on a collision
    * course with the current robot. Doesn't take into account objects between the bullet and this robot.
    *
    * @param bullet The bullet in question
    * @return True if the line of the bullet's path intersects with this robot's current position.
    */
   static boolean willCollideWithMe(BulletInfo bullet) {
       MapLocation myLocation = rc.getLocation();

       // Get relevant bullet information
       Direction propagationDirection = bullet.dir;
       MapLocation bulletLocation = bullet.location;

       // Calculate bullet relations to this robot
       Direction directionToRobot = bulletLocation.directionTo(myLocation);
       float distToRobot = bulletLocation.distanceTo(myLocation);
       float theta = propagationDirection.radiansBetween(directionToRobot);

       // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
       if (Math.abs(theta) > Math.PI/2) {
           return false;
       }

       // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
       // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
       // This corresponds to the smallest radius circle centered at our location that would intersect with the
       // line that is the path of the bullet.
       float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

       return (perpendicularDist <= rc.getType().bodyRadius);
   }
}
