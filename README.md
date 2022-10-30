# IRTX JSprit Implementation

## Introduction

The JSprit model makes use of the open-source route optimization library
JSprit:

> https://jsprit.github.io/

During the LEAD project it has been adapted to the specific use case of the
Lyon living lab, but with the ambition to provide a generic route and fleet
size optimization tool that can be applied to other cases. The individual uses
and new developments around JSprit in LEAD have been documented in

> Mahmoud, A., Chouaki, T., Hörl, S., Puchinger, J., 2022. Adapting JSprit for the Electric Vehicle Routing Problem with Recharging: Implementation and Benchmark. International Journal for Traffic and Transport Engineering 12 (3), 340-351. http://dx.doi.org/10.7708/ijtte2022.12(3).04

The model has been streamlined to work on configurable scenarios that define:

- Vehicle types with their individual properties such as daily and per-distance costs, speeds, energy consumption and emissions
- Operators with their individual distribution centers (by coordinate) and demand to fulfill during one day (by coordinate) and a list of vehicle types that are available at their depots

Optionally, an Urban Consolidation Center (UCC) can be defined with its location (by coordinate) and its available vehicle types. For each conventional operator (see above), it can be defined whether deliveries should be shipped through the UCC if it is defined.

The model, hence, allows to build a rich set of shipment scenarios. In the baseline case, various operators can be defined. The model will then optimize the fleet composition (based on the available vehicle types) and the driven distance by minimizing the total cost of each operator individually. After, a consolidation scenario can be developed in which all or some operators need to pass their deliveries through the UCC, which, analogously to the operators, minimizes its total cost.

Additional scenarios can be constructed, for instance, by changing the cost structures
and observing trade-offs between vehicle types.

Routing-based costs are either approximated using Euclidean distance or calculated directly based on OpenStreetMap road network data.

## Requirements

### Software requirements

To run the model, the environment needs to be prepared:

- A `conda` or `mamba` environment needs to be set up in which the Python code of the model is run. The LEAD repository provides `environment.yml` which describes the `conda` environment and all dependencies.

- Note that some of the dependencies installed via `conda > pip` need a recent compiler available on the system. Additionally, the MATSim instances that are run in the pipeline need to have access to the fonts available on the system. On an Ubunutu system, it suffices to `apt install build-essential fontconfig`.

- A `Java` runtime needs to be present on the executing machine. It is recommended to set up an **Adoptium OpenJDK 11** (https://adoptium.net).

- A recent version of `maven` needs to be installed, version `3.6.3` has been tested: https://maven.apache.org/

- Finally, `osmosis` needs to be installed, version `0.48.2` has been tested: https://github.com/openstreetmap/osmosis/releases

It is recommended to set up the environment on a Linux machine, the following
executables should then be callable from the command line: `java`, `mvn`, `osmosis`.

### Input / Output

#### Input

The model needs various input data sets. First, network data needs to be provided
to perform the distance routing, this includes defining the perimeter of the
study area. Second, information on the operators needs to be provided including
their distribution center locations and demand data. Finally, the operator
data is merged into a scenario configuration. To ease the process of setting
up a scenario, we provide a command line tool that makes it easy to join this
operator information and change values in the scenario configuration using
command line parameters.

**Network**

In order to define the network topology on which the deliveries will be routed,
data from OpenStreetMap needs to be provided in `pbf` format. Regular snapshots
of OpenStreetMap data are available publicly from [Geofabrik](https://geofabrik.de/).

Processing the whole OpenStreetMap each time the model is run would take too
much time, so it is recommended to trim the data to the specific use case
area. This task can be achieved by the `osmosis` command line tool, which,
however needs a specific non-standard format that describes the perimeter to
cut. For that purpose, we provide the `prepare_perimeter.py` script, which
can be called as follows once one has entered the `conda` environment:

```bash
python3 prepare_perimeter.py \
  --input-path /path/to/perimeter.gpkg \
  --output-path /path/to/perimeter.poly
```

The input perimeter can be provided in any common geographic data format that
is understood by `geopandas` / `fiona` such as Shape file (`shp`) or GeoPackgage (`gpkg`).

The resulting `poly` file can then be passed to `osmosis` to cut the case study
area. The command is packaged in `prepare_osm.sh` which can be called as follows:

```bash
sh prepare_osm.sh \
  /path/to/openstreetmap.osm.pbf \
  /path/to/perimeter.poly \
  /path/to/reduced.osm.pbf
```

*Note that these steps only need to be performed once per use case. Once the OpenStreetMap data has been converted they serve as input to any subsequent model execution.* Concrete examples and data for the Lyon living lab of the LEAD project are given further below.


**Operators**

Data for multiple operators that are active on the territory can be provided. Each
operator is represented in a separate `json` file with the following exemplary
format:

```json
{
  "id": "my_operator",
  "center": { "lat" : 45.7327, "lng" : 4.8245 },
  "vehicle_types": ["van"],
  "shipment_type": "delivery",
  "consolidation_type": "none",
  "demand": [
    { "lat" : 45.741532014731064, "lng" : 4.822740554809571 },
    { "lat" : 45.74407774585035, "lng" : 4.816946983337403 },
    { "lat" : 45.73954027356843, "lng" : 4.818427562713624 }
  ]
}
```

First, an internal identifier is given for every opeartor (`id`), second, the
location of its distribution center is given as latitude and longitude. After,
a list of *available* vehicle types is defined (see below). The file ends with
the `demand` section which describes the individual shipments that need to be
fulfilled by the operator during one day by coordinate.

An important part is the definition of the *shipment type* and the
*delivery type*. The *shipment type* describes how goods are moved between
the distribution center and the final destinations. If it is set to `delivery`,
the operator will deliver (using the available vehicle types) all goods to the
destinations. In case it is set to `pickup`, each destination will use one of
the available vehicle types to pick up the delivery. Usually, this clearly leads
to a much higher number of vehicles and movements (but allows, for instance, the
representation of common logistics schemes for construction sites today). Finally,
the value can be set to `none` indicating that no direct movements from the
distribution center. This allows, for instance, to construct scenarios in which
all operations are entirely moved to the a third-party consolidation center.

The *consolidation type* is set to `none` by default. This means that the relations
between the distribution center and the destinations are handled directly by the
receivers or the operator. In case it is set to `delivery`, the goods will be
delivered to the final destinations from the UCC using its defined vehicle types.
In case it is set to `pickup`, the receivers will use their defined vehicle types
to pick up the goods at the UCC. In any case, if any UCC *consolidation type* is
chosen, the *shipment type* of the operator defines how the goods arrive at the
UCC in the first place. This means that one can define situations in which the
operator either delivers the goods to the UCC, the UCC picks up the goods at
the operator's distribution center, or no interaction between the operator center
and the UCC is happening.

**Scenario**

A scenario is described through a `json` file of the following format:

```json
{
  "vehicle_types": [
    {
      "id": "van",
      "capacity" : 20,
      "cost_per_day_EUR" : 85.0,
      "cost_per_km_EUR" : 0.15,
      "co2_per_km_g" : 130.0,
      "energy_per_km_Wh" : 120.0,
      "speed_km_h": 40.0,
      "euclidean_distance_factor": 1.3
    }
  ],
  "ucc": {
    "location": { "lat" : 45.74243052132232, "lng" : 4.824800491333009 },
    "vehicle_types": [ "cargobike", "van" ]
  },
  "operators": []
}
```

On top, multiple vehicle types with individual identifiers can be defined that
differ in their capacity (number of items that can be carried), their cost
structure, emissions, energy consumption, and speed. The `euclidean_distance_factor`
is added to any distance in case the model is run *without* specific network data
(see below). After, the characteristics
of the `ucc` are defined by defining a location in latitude and longitude and its
available vehicle types.

Finally, a list of operators is given. Each operator follows exactly the configuration
scheme as presented above. One could, hence, manually build a complete scenario
by copy-pasting the operator information directly into the scenario file. To ease
the process, a command line-based utility is provided alongside the model (see below).

#### Configuration

To configure a scenario, the provided script `prepare_scenario.py` can be used
which is called as follows:

```bash
python3 prepare_scenario.py \
  --scenario-path /path/to/base_scenario.json \
  --output-path /path/to/output_scenario.json \
  --operator-path /path/to/operator1.json \
  --operator-path /path/to/operator2.json \
  --operator-path /path/to/operator3.json \
  --shipment-type:operator1 pickup \
  --consolidation-type:operator2 delivery \
  --vehicle-type:van:cost_per_km_EUR 0.3
```

The scenario preparation script has the following **mandatory** parameters:

The **mandatory** parameters are detailed in the following table:

Parameter             | Values                            | Description
---                   | ---                               | ---
`--scenario-path`          | String                            | Path to the baseline scenario
`--output-path`         | String                            | Path to the updated output scenario


The following **optional** parameters exist that can be configured.:

Parameter             | Values                            | Description
---                   | ---                               | ---
`--operator-path`         | String             | Integrates a new operator defined in a `json` file into the scenario. *Can be set multiple times to integrate multiple operators.*
`--shipment-type:{operator}`         | `delivery`* or `pickup`             | Sets the shipment type for operator `{operator}` (see above)
`--consolidation-type:{operator}`             | `none`* or `delivery` or `pickup`              | Sets the consolidation type for operator `{operator}` (see above)
`--driver-salary:{operator}`             | Real              | Sets the daily salary per driver in EUR
`--vehicle-type:{vt}:{property}`             | Any              | Sets a property of vehicle type `{vt}`, for instance `speed_km_h`

Concrete use cases for the utility in the context of the Lyon living lab will be
provided further down in the *Standard scenarios* section.

#### Output

The output of the model is provided as a `json` file of the following format:

```json
{
  "cost_EUR" : 235.27880397887634,
  "energy_kWh" : 0.2788039788763593,
  "co2_kg" : 0.2788039788763593,
  "distance_km" : 0.2788039788763594,
  "runtime_s" : 7.006087272,
  "routes" : []
}
```

It contains various KPIs such as the total cost of the system, the energy
consumed, the CO2 emitted, and the distance driven. Furthermore, the runtime
of the model is given.

The `routes` part gives a more detailed analysis per vehicle, in the following
format:

```json
{
  "cost_EUR" : 65.27,
  "energy_kWh" : 0.27,
  "co2_kg" : 0.27,
  "distance_km" : 0.27,
  "vehicle_type" : "cargobike",
  "carrier" : "operator_id",
  "trajectory" : [
    { "lat" : 45.74243052132232, "lng" : 4.824800491333009, "t0" : 0.0, "t1": 60.0 },
    { "lat" : 45.74199624494206, "lng" : 4.820101261138917, "t0" : 900.0, "t1": 960.0 }
  ]
}
```

The first fields give the same information as the overall KPI analysis, but
specifically for an individual vehicle. The vehicle type of that vehicle is
given after, as well as the operator to which it belongs. The `carrier` may
be `$ucc$` to indicate that the vehicle is operated by the UCC. Finally, a
detailed trajectory of the vehicle including coordinates and timestamps is
given.

# Building the model

The model is provided as Java code. To run it, it first needs to be built using
the Maven build system. For that purpose, one needs to enter the `java` directory
of the LEAD repository and package and call `mvn package`:

```bash
cd /irtx-jsprit/java
mvn package
```

The build process should download all necessary Maven dependencies including
the JSprit library and finish without errors. After, the built model should be
present in

```
/irtx-jsprit/java/target/lead-jsprit-1.0.0.jar
```

The `jar` file can be saved in a fixed location. As long as the model is not
changed, it can be reused for multiple model runs. To test whether the `jar` has
been build successfully, call

```bash
java -cp /path/to/lead-jsprit-1.0.0.jar fr.irtx.lead.jsprit.RunVerification
```

which should respond by the message `It works!`.

# Running the model

Once the model is built, it can be called the following way:

```bash
java -cp /path/to/lead-jsprit-1.0.0.jar fr.irtx.lead.jsprit.RunSolver \
  --problem-path /path/to/scenario.json \
  --solution-path /path/to/solution.json \
  --crs EPSG:2154 \
  --osm-path /path/to/scenario.osm.pbf
```

The first line is mandatory with the path to the built `jar` file that needs
to be adapted. The following lines represent parameters. The **mandatory**
parameters are detailed in the following table:

Parameter             | Values                            | Description
---                   | ---                               | ---
`--problem-path`          | String                            | Path to the scenario file
`--solution-path`         | String                            | Path to where the result will be saved
`--crs`         | String                            | A geographic projection

The coordinate reference system (CRS) must be provided such that the model can
performed Euclidean distance calculations on the coordinates and network data. The
projection is use-case specific. For France, the standard geographic projection is
`EPSG:2154`.

The following **optional** parameters are available:

Parameter             | Values                            | Description
---                   | ---                               | ---
`--osm-path`          | String                            | Path to the filtered OSM data.
`--freespeed-factor`  | Real (default `0.7`)        | A factor that is added to the nominative road speeds to simulate congestion

*Note that if no path to the OpenStreetMap data is given, the model will perform Euclidean distance calculations with a per-vehicle-type factor.*

Finally, **technical** parameters exist that can be configured:

Parameter             | Values                            | Description
---                   | ---                               | ---
`--random-seed`       | Integer (default `1234`)          | Allows to perform ensemble runs by providing different initialization seeds of the optimization
`--iterations`        | Integer (default `10000`)         | Sets the number of iterations per operator with higher accuracy with increasing values (but also higher runtime)
`--threads`           | Integer (default `1`)             | Allows to set the number of threads that are used for the optimization

## Standard scenarios

For the Lyon living lab, some standard scenarios can be run. Initially, the
preparationl steps need to be performed. After, individual scenarios can
be evaluated/

### Preparation

**Network**

In the specific
case for Lyon, the latest snapshot of the Rhône-Alpes region provided by Geofabrik
can be used:

> https://download.geofabrik.de/europe/france/rhone-alpes.html

For the specific case of Lyon, the perimeter is provided in the `/scenario_lyon` directory
in the LEAD repository. Hence, the correct `poly` file can be created by calling

```bash
python3 prepare_perimeter.py \
  --input-path /irtx-jsprit/scenario_lyon/perimeter.gpkg \
  --output-path /irtx-jsprit/data/perimeter.poly
```

This file can be used with `osmosis` to cut the relevant perimter for the Confluence
study area in Lyon with the resulting file `scenario.osm.pbf`:

```bash
sh prepare_osm.sh \
  /irtx-jsprit/data/rhone-alpes-latest.osm.pbf \
  /irtx-jsprit/data/perimeter.poly \
  /irtx-jsprit/data/scenario.osm.pbf
```

**Operator**
For the Lyon living lab, an operator file is available in `/scenario_lyon` as
`rexel.json`. This file does not exactly describe the actual demand of the operator.
Respective data will be integrated in the final platform.

Additionally, the downstream parcel generation model and its respective
connector model can be used to generate a parcel shipment operator (`laposte_*.json`)
based on synthetic population data.

### Scenario execution

For the Lyon living lab, a scenario template is provided in the LEAD repository
as `/irtx-jsprit/scenario_lyon/template.json`. Different configurations based on
this template are described further below. The resulting configured scenario
files can then be run using the following command:

```bash
java -cp /irtx-jsprit/java/target/lead-jsprit-1.0.0.jar fr.irtx.lead.jsprit.RunSolver \
  --problem-path /irtx-jsprit/data/scenario_*.json \
  --solution-path /irtx-jsprit/data/solution_*.json \
  --crs EPSG:2154 \
  --osm-path /irtx-jsprit/data/scenario.osm.pbf
```

In each case, a different `/irtx-jsprit/data/scenario.json` can be generated using
the configuration tool:

**Baseline 2022**

Note that the output from the parcel generation connector should be located
in `/irtx-parcels-to-jsprit/output` in this example.

```bash
python3 prepare_scenario.py \
  --scenario-path scenario_lyon/template.json \
  --output-path data/scenario_baseline_2022.json \
  --operator-path scenario_lyon/rexel.json \
  --operator-path /irtx-parcels-to-jsprit/output/laposte_2022.json \
  --shipment-type:rexel pickup \
  --shipment-type:laposte delivery \
  --consolidation-type:rexel none \
  --consolidation-type:laposte none \
  --driver-salary:rexel 0.0
```

The driver salary for Rexel is put to zero, because it is a pick-up service,
which means that employees are employed independent of their pick-up tasks.

**UCC 2022**

In this scenario consolidation is integrated by forcing the deliveries to be routed
through the UCC. The postal distribution center is relcoated to the UCC. This is done
by changing the last four lines.

```bash
python3 prepare_scenario.py \
  --scenario-path scenario_lyon/template.json \
  --output-path data/scenario_ucc_2022.json \
  --operator-path scenario_lyon/rexel.json \
  --operator-path /irtx-parcels-to-jsprit/output/laposte_2022.json \
  --shipment-type:rexel delivery \
  --shipment-type:laposte none \
  --consolidation-type:rexel delivery \
  --consolidation-type:laposte delivery
```

In this example, Rexel needs a driver, so the salary is kept at its default
value.

**UCC 2030**

In this scenario the parcel demand for 2030 is used.

```bash
python3 prepare_scenario.py \
  --scenario-path scenario_lyon/template.json \
  --output-path data/scenario_ucc_2030.json \
  --operator-path scenario_lyon/rexel.json \
  --operator-path /path/to/parcels/laposte_2030.json \
  --shipment-type:rexel delivery \
  --shipment-type:laposte none \
  --consolidation-type:rexel delivery \
  --consolidation-type:laposte delivery
```

**Other scenarios**

Other scenarios can be constructed by changing parameters in the upstream
models (total number of parcels, for instance), by replacing the operator
data sets for other years or even adding new operators, or by changing the
vehicle parameters.

In the platform a user could, for instance, a list of prepared operator data
sets that can then be tested in combination using the model.
