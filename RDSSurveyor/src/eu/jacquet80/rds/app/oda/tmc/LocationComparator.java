package eu.jacquet80.rds.app.oda.tmc;

import java.util.Comparator;

/**
 * @brief A comparator for sorting TMC locations.
 * 
 * This comparator sorts locations based on their order along a linear feature, according to
 * the direction given in the constructor. If the two locations have a parent relationship
 * (e.g. a segment and a point on that segment), the parent is sorted before the child. If two
 * locations are not part of the same linear feature and have no parent-child relationship,
 * they are not sortable and considered equal.
 */
public class LocationComparator implements Comparator<TMCLocation> {
	private final boolean negativeDirection;

	public LocationComparator(boolean negativeDirection) {
		super();
		this.negativeDirection = negativeDirection;
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 * 
	 * @return -1 if lhs < rhs, +1 if lhs > rhs, 0 otherwise
	 */
	@Override
	public int compare(TMCLocation lhs, TMCLocation rhs) {
		/* trivially, if locations are equal, return 0 */
		if (lhs.equals(rhs))
			return 0;
		
		/* handle parent relationships */
		if (rhs.isChildOf(lhs))
			return -1;
		if (lhs.isChildOf(rhs))
			return 1;

		/* if both are points, cycle through offsets */
		if ((lhs instanceof TMCPoint) && (rhs instanceof TMCPoint)) {
			TMCPoint lneg = ((TMCPoint) lhs).getNegOffset();
			TMCPoint lpos = ((TMCPoint) lhs).getPosOffset();
			while (!rhs.equals(lneg) && !rhs.equals(lpos) && (lneg != null || lpos != null)) {
				if (lneg != null)
					lneg = lneg.getNegOffset();
				if (lpos != null)
					lpos = lpos.getPosOffset();
			}

			if (rhs.equals(lneg))
				return negativeDirection ? -1 : 1;
			else if (rhs.equals(lpos))
				return negativeDirection ? 1 : -1;
			else
				return 0;
		}

		/* if both are segments, do the same but also consider nested segments */
		if ((lhs instanceof Segment) && (rhs instanceof Segment)) {
			Segment lneg = ((Segment) lhs).getNegOffset();
			Segment lpos = ((Segment) lhs).getPosOffset();
			while (!rhs.equals(lneg) && !rhs.equals(lpos) && (lneg != null || lpos != null)) {
				if (lneg != null)
					lneg = lneg.getNegOffset();
				if (lpos != null)
					lpos = lpos.getPosOffset();
			}

			if (rhs.equals(lneg))
				return negativeDirection ? -1 : 1;
			else if (rhs.equals(lpos))
				return negativeDirection ? 1 : -1;
			else {
				int res = 0;
				if (((Segment) lhs).segment != null)
					res = compare(((Segment) lhs).segment, rhs);
				if ((res == 0) && (((Segment) rhs).segment != null))
					res = compare (lhs, ((Segment) rhs).segment);
				return res;
			}
		}

		/* nothing applies, items are not sortable */
		return 0;
	}

}