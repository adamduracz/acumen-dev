#0 {
  _3D = ("Cylinder", (-1, 0, 0), (0.010000, 1), (1, 0, 0), (1.570800, 0, 0)),
  className = Main,
  mode = "push",
  simulator = #0.0,
  system = #0.1,
  t = 0,
  t' = 1
}
#0.0 { className = Simulator, time = 0.000000 }
#0.1 {
  F = 0,
  Fs = 0,
  _3D = (("Box", (0, 0, 0), (0.300000, 0.300000, 0.300000), (0, 0, 1), (0, 0, 0)), ("Cylinder", (-0.500000, 0, 0), (0.010000, 1), (0, 1, 0), (0, 0, 1.570800))),
  c = 1,
  className = SpringMass,
  k = 10,
  m = 3,
  x = 0,
  x' = 0,
  x'' = 0,
  x0 = 0
}
------------------------------0
#0 {
  _3D = ("Cylinder", (-1, 0, 0), (0.010000, 1), (1, 0, 0), (1.570800, 0, 0)),
  className = Main,
  mode = "free_motion",
  simulator = #0.0,
  system = #0.1,
  t = 0.500000,
  t' = 1
}
#0.0 { className = Simulator, time = 0.500000 }
#0.1 {
  F = 10,
  Fs = 5.011819,
  _3D = (("Box", (-0.368105, 0, 0), (0.300000, 0.300000, 0.300000), (0, 0, 1), (0, 0, 0)), ("Cylinder", (-0.684053, 0, 0), (0.010000, -0.631895), (0, 1, 0), (0, 0, 1.570800))),
  c = 1,
  className = SpringMass,
  k = 10,
  m = 3,
  x = -0.368105,
  x' = -1.330766,
  x'' = -1.662727,
  x0 = 0
}
------------------------------51
#0 {
  _3D = ("Cylinder", (-1, 0, 0), (0.010000, 1), (1, 0, 0), (1.570800, 0, 0)),
  className = Main,
  mode = "free_motion",
  simulator = #0.0,
  system = #0.1,
  t = 10.010000,
  t' = 1
}
#0.0 { className = Simulator, time = 10.010000 }
#0.1 {
  F = 0,
  Fs = -1.393992,
  _3D = (("Box", (0.156045, 0, 0), (0.300000, 0.300000, 0.300000), (0, 0, 1), (0, 0, 0)), ("Cylinder", (-0.421978, 0, 0), (0.010000, -1.156045), (0, 1, 0), (0, 0, 1.570800))),
  c = 1,
  className = SpringMass,
  k = 10,
  m = 3,
  x = 0.156045,
  x' = -0.166457,
  x'' = -0.464664,
  x0 = 0
}
------------------------------1002
