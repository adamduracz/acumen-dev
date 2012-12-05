package acumen.interpreters.enclosure

/** Parameters for solve-hybrid */
case class Parameters(
  precision: Int,
  startTime: Double, // simulation start time
  endTime: Double, // simulation end time
  solveVtInitialConditionPadding: Double, // padding for initial condition in solveVt
  extraPicardIterations: Int, // number of extra Picard iterations in solveVt
  maxPicardIterations: Int, // maximum number of Picard iterations in solveVt
  maxEventTreeSize: Int, // maximum event tree size in solveVtE, gives termination condition for tree enlargement
  minTimeStep: Double, // minimum time step size
  maxTimeStep: Double, // maximum time step size
  minImprovement: Double, // minimum improvement of enclosure
  splittingDegree: Int // number of pieces to split each initial condition variable 
  ) {
  implicit val rnd = Rounding(precision)
  val simulationTime = Interval(startTime, endTime)
}