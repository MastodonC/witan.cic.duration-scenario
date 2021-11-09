# witan.cic.duration-scenario

Duration scenario modelling.

## Inputs

Two inputs are required to execute a duration scenario:

1) An EDN output from a witan.cic model, usually called `periods.edn`
2) A CSV description of the scenario you want to apply


### Periods.edn

When running the witan.cic model, ensure that the config specifies `:output-projection-periods? true`.
This will cause a `periods.edn` file to be written to the output directory.
The file contents is similar to the `episodes.csv` output, but the EDN structure is more convenient to work with.


### Scenario.csv

The scenario CSV should contain 3 columns labelled placement, duration-cap-days, and probability-cap-applies.

| placement | duration-cap-days | probability-cap-applies |
|-----------|-------------------|-------------------------|
| Q1        | 750               | 0.5                     |

The scenario must have at least one row, but can have up to N rows. A placement should not appear more than once.
Each row specifies a maximum duration in days that a child can remain in the placement, and a probability that maximum duration is enforced.
For example, any child remaining in Q1 for longer than 750 days has a 50% chance of having the duration cap applied.
If the cap is applied, then the child leaves care after remaining in Q1 for 750 days.
Any child who doesn't ever enter Q1, or who remains in Q1 for less than the duration cap, won't be affected by the scenario.

## Usage

There are two distinct ways to run the scenario: in a REPL or via the main class.

### REPL usage

The main REPL function is `witan.cic.duration-scenario.core/apply-duration-scenario-rules`.
The function expects to receive 3 arguments:
* The EDN contents of periods.edn
* The sequence of maps representing the parsed scenario.csv
* A random seed long

`witan.cic.duration-scenario.main/run-duration-scenario!` illustrates how to the input files and execute the duration scenario.

### Main class usage

The main class can be executed using the Clojure CLI with the following invocation: `clj -m witan.cic.duration-scenario.main duration-scenario -c [path/to/config.edn]`.

Input and output locations are specified in the config file and an example file is saved to `/data/demo/config.edn`:

```clojure
{:input-periods [periods.edn]
 :input-parameters [scenario.csv]
 :output-periods [scenario-periods.nippy]
 :output-periods-csv [periods.csv]
 :random-seed 42}
 ```
 
The two output files `output-periods` and `output-periods-csv` serve different purposes.
* The `output-periods` file is a nippy representation of the return value of the `apply-duration-scenario-rules` function, including the `:marked` key (see below)
* The `output-periods-csv` file is a CSV subset of the above suitable for passing to the charts in witan.cic.viz excluding those periods marked remove.

## Output

The `apply-duration-scenario-rules` function adds a `:marked` key to each period which may contain one of the following values:

* :applied
* :remove
* :not-applied
* :not-applicable

**applied**: Periods marked with applied have had the duration cap applied.
They represent a transformation of an input period which was matched by a duration scenario rule.

**remove**: Every applied period will have a corresponding remove period.
These periods are the untransformed input periods which matched a duration scenario rule.
They are included in the output to enable comparative before/after analysis.

**not-applied**: If a period _might_ have matched a scenario rule but didn't (since the `probability-cap-applies` will usually be less than 1.0), then it will be marked with not-applied.
This allows us to verify that scenario matching is working as expected because the proportion of applied vs not-applied should closely match the cap probability.

**not-applicable**: If a period couldn't have matched any of the scenario rules either because the placement sequence didn't contain a candidate placement or because the time in that setting never execeeded the duration cap, the period will be marked not-applicable.

## License

Copyright © 2021 Mastodon C
