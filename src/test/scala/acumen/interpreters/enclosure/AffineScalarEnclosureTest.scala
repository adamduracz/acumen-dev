package acumen.interpreters.enclosure

import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalacheck.Gen._
import org.scalacheck.Prop._
import org.scalacheck.Properties

import AffineScalarEnclosure._
import Box._
import Generators._
import Interval._
import Types._

object AffineScalarEnclosureTest extends Properties("AffineScalarEnclosure") {

  //  import TestingContext._
  implicit val rnd = Rounding(10)

  /* Type synonyms */

  type BinaryASEOp = (AffineScalarEnclosure, AffineScalarEnclosure) => AffineScalarEnclosure

  /* Properties */

  property("constant AffineScalarEnclosure has arity 0") =
    forAll(genBox, genInterval) { (box, interval) =>
      AffineScalarEnclosure(box, interval).arity == 0
    }

  property("projection AffineScalarEnclosure has arity 1") =
    forAll(posNum[Int]) { dim =>
      forAll(genDimBox(dim)) { box =>
        forAll(oneOf(box.keySet.toList)) { name =>
          AffineScalarEnclosure(box, name).arity == 1
        }
      }
    }

  property("projections act as identity functions on normalized boxes") =
    forAllNoShrink(genDimBox(1)) { dom =>
      val name = dom.keySet.toList(0)
      val ndom = Box.normalize(dom)
      val proj = AffineScalarEnclosure.apply(ndom, name)
      forAllNoShrink(genThinSubBox(ndom)) { subnDom =>
        proj(subnDom) == subnDom(name)
      }
    }

  property("projections safely approximate identity functions") =
    forAllNoShrink(genDimBox(1)) { dom =>
      val name = dom.keySet.toList(0)
      val proj = AffineScalarEnclosure.apply(dom, name)
      forAllNoShrink(genSubBox(dom)) { x =>
        proj(x) contains x(name)
      }
    }

  property("an enclosure is the sum of its constantTerm and linearTerms") =
    forAll { (f: AffineScalarEnclosure) =>
      f.constantTerm + f.linearTerms == f
    }

  property("monotonicity of enclosure evaluation") =
    forAll(choose(1, 10)) { dim =>
      forAll(genDimBox(dim)) { dom =>
        forAll(genSubBox(dom), genBoxAffineScalarEnclosure(dom)) { (box, f) =>
          forAll(genSubBox(box)) { subbox =>
            f(box) contains f(subbox)
          }
        }
      }
    }

  property("sanity test of enclosure evaluation") =
    {
      val dom = Box("t" -> Interval(1, 2))
      val t = AffineScalarEnclosure(dom, "t")
      t(dom) == dom("t")
    }

  property("sanity test of enclosure range") =
    {
      val dom = Box("t" -> Interval(1, 2))
      val t = AffineScalarEnclosure(dom, "t")
      t.range == dom("t")
    }

  property("range monotonicity") =
    forAll(choose(1, 10)) { dim =>
      forAll(genDimBox(dim)) { dom =>
        forAll(genSubBox(dom), genBoxAffineScalarEnclosure(dom)) { (x, f) =>
          f.range contains f(x)
        }
      }
    }

  property("sanity test for enclosure containment") =
    {
      val dom = Box("t" -> Interval(0, 1))
      val t = AffineScalarEnclosure(dom, "t")
      t contains t
    }

  property("enclosure containment implies point-wise containment") =
    forAllNoShrink(choose(1, 10)) { dim =>
      forAllNoShrink(genDimBox(dim)) { dom =>
        forAllNoShrink(genSubBox(dom), genBoxAffineScalarEnclosure(dom)) { (box, f) =>
          forAllNoShrink(genSubAffineScalarEnclosure(f)) { subf =>
            (f contains subf) ==> (f(box) contains subf(box))
          }
        }
      }
    }

  property("enclosure containment soundness") =
    forAllNoShrink(choose(1, 10)) { dim =>
      forAllNoShrink(genDimBox(dim)) { dom =>
        forAllNoShrink(genBoxAffineScalarEnclosure(dom)) { f =>
          forAllNoShrink(genSubAffineScalarEnclosure(f)) { subf =>
            f contains subf
          }
        }
      }
    }
  
  property("numeric operations monotonicity") =
    //TODO Add testing of division and operation variants (intervals, scalars)
    forAll(choose(1, 10),
      oneOf(Seq((_ + _), (_ - _), (_ * _))): Gen[BinaryASEOp]) { (dim, bop) =>
        forAllNoShrink(genDimBox(dim)) { dom =>
          forAllNoShrink(
            genBoxAffineScalarEnclosure(dom),
            genBoxAffineScalarEnclosure(dom)) { (x, y) =>
              forAllNoShrink(
                genSubAffineScalarEnclosure(x),
                genSubAffineScalarEnclosure(y)) { (subx, suby) =>
                  bop(x, y) contains bop(subx, suby)
                }
            }
        }
      }

  property("monotonicity of range w.r.t. restriction") =
    forAllNoShrink(choose[Int](1, 10)) { dim =>
      forAllNoShrink(genDimBox(dim)) { dom =>
        forAllNoShrink(
          genSubBox(dom),
          genBoxAffineScalarEnclosure(dom)) { (subDom, e) =>
            e.range contains e.restrictTo(subDom).range
          }
      }
    }

  property("upper bound monotonicity of approximations of quadratics on normalized domains") =
    forAllNoShrink(choose[Int](1, 10)) { dim =>
      forAllNoShrink(genDimBox(dim)) { dom =>
        forAllNoShrink(
          genSubBox(dom),
          oneOf(dom.keys.toList)) { (subDom, name) =>
            quadratic(dom, name).restrictTo(subDom) contains quadratic(subDom, name).high
          }
      }
    }

  property("affine enclosures safely approximate mixed terms") =
    false // TODO Implement property

  property("affine enclosure of primitive function") =
    false // TODO Implement property

  property("collapsing removes the collapsed variable") =
    forAll(choose(1, 10)) { dim =>
      forAll(genDimBox(dim)) { dom =>
        forAll(genBoxAffineScalarEnclosure(dom), oneOf(dom.keys.toSeq)) { (f, name) =>
          val collapsed = f.collapse(name)
          !collapsed.domain.contains(name) && !collapsed.coefficients.contains(name)
        }
      }
    }

  property("safety of enclosure collapsing") =
    forAll(choose(1, 10)) { dim =>
      forAll(genDimBox(dim)) { dom =>
        forAll(genBoxAffineScalarEnclosure(dom), oneOf(dom.keys.toSeq)) { (f, name) =>
          val fnoname = f.collapse(name)
          val collapsed = AffineScalarEnclosure(f.domain, f.normalizedDomain, fnoname.constant, fnoname.coefficients)
          collapsed contains f
        }
      }
    }

}

object AffineScalarEnclosureUnitTest extends Properties("AffineScalarEnclosure.UnitTest") {

  import TestingContext._

  /* Unit tests */

  property("scala ASE expressions - sums") = {
    val unit = Interval(0, 1)
    val u = Box("t" -> unit)
    val x = AffineScalarEnclosure(u, "t")
    x(u) == unit &&
      (x + x)(rnd)(u) == 2 * unit &&
      (x * unit)(rnd)(u) == unit &&
      (x * unit)(rnd)(u) == unit &&
      (x * 5)(rnd)(u) == 5 * unit
  }

  property("enclosure scala evaluation failing test case") = {
    // Should result in [15.75,25]
    val box = Box("l" -> Interval(4, 5))
    val l = AffineScalarEnclosure(box, "l")
    val e = l * l
    println("e.enclosureEval(box)" + e(box))
    Interval(15.75, 25) contains e(box)
  }

}

