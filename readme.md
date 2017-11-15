Summary
=======

The source code and the datasets used for the experiments in my "Relational Aggregation using First-Order Deep Neural Models" paper to appear at AAAI-18. 

## How to access the datasets:

The MovieLens 1M dataset is in `datasets/ml-1m` folder. It can be also downloaded from the [this link](https://grouplens.org/datasets/movielens/). The Yelp! and KDD datasets are in `datasets/yelp_mc.db` and `datasets/KDD15_123.db` respectively. Please read the terms of use for these datasets before using them.

## How to run the code:

Make sure you have java JDK installed on your machine. Compile the code using the following command (note that the command starts after $):

    $ javac *.java

Then for each experiment, run the corresponding class:

    $ java <ClassName>

For instance to run the experiemnts on the Yelp! dataset, run:

    $ java YelpMC

The other classes are called `MovieLensGender`, `MovieLensAge`, and `KDD`.

Contact
=======

Seyed Mehran Kazemi

Computer Science Department

The University of British Columbia

201-2366 Main Mall, Vancouver, BC, Canada (V6T 1Z4)  

<http://www.cs.ubc.ca/~smkazemi/>  

<smkazemi@cs.ubc.ca>


License
=======

Licensed under the GNU General Public License Version 3.0.
<https://www.gnu.org/licenses/gpl-3.0.en.html>


Copyright (C) 2017  Seyed Mehran Kazemi


