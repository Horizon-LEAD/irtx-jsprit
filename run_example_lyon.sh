set -e

## Prepare
cd /home/ubuntu/irtx-jsprit
mkdir /home/ubuntu/irtx-jsprit/output

## Create environment
conda create -f environment.yml -n jsprit

## Activate environment
conda activate jsprit

## Prepare perimeter
python3 prepare_perimeter.py \
  --input-path /home/ubuntu/irtx-jsprit/data/perimeter_lyon.gpkg \
  --output-path /home/ubuntu/irtx-jsprit/output/perimeter_lyon.poly

## Cut OSM data
sh prepare_osm.sh \
  /home/ubuntu/irtx-jsprit/input/rhone-alpes-latest.osm.pbf \
  /home/ubuntu/irtx-jsprit/output/perimeter_lyon.poly \
  /home/ubuntu/irtx-jsprit/output/scenario_lyon.osm.pbf

## Prepare scenarios
python3 prepare_scenario.py \
  --scenario-path /home/ubuntu/irtx-jsprit/data/template_lyon.json \
  --output-path /home/ubuntu/irtx-jsprit/output/scenario_baseline_2022.json \
  --operator-path /home/ubuntu/irtx-jsprit/data/rexel_lyon.json \
  --operator-path /home/ubuntu/irtx-parcels-jsprit-connector/output/laposte_2022.json \
  --shipment-type:rexel pickup \
  --shipment-type:laposte delivery \
  --consolidation-type:rexel none \
  --consolidation-type:laposte none \
  --driver-salary:rexel 0.0

python3 prepare_scenario.py \
  --scenario-path /home/ubuntu/irtx-jsprit/data/template_lyon.json \
  --output-path /home/ubuntu/irtx-jsprit/output/scenario_ucc_2022.json \
  --operator-path /home/ubuntu/irtx-jsprit/data/rexel_lyon.json \
  --operator-path /home/ubuntu/irtx-parcels-jsprit-connector/output/laposte_2022.json \
  --shipment-type:rexel delivery \
  --shipment-type:laposte none \
  --consolidation-type:rexel delivery \
  --consolidation-type:laposte delivery

python3 prepare_scenario.py \
  --scenario-path /home/ubuntu/irtx-jsprit/data/template_lyon.json \
  --output-path /home/ubuntu/irtx-jsprit/output/scenario_ucc_2030.json \
  --operator-path /home/ubuntu/irtx-jsprit/data/rexel_lyon.json \
  --operator-path /home/ubuntu/irtx-parcels-jsprit-connector/output/laposte_2030.json \
  --shipment-type:rexel delivery \
  --shipment-type:laposte none \
  --consolidation-type:rexel delivery \
  --consolidation-type:laposte delivery

## Build JSprit
cd /home/ubuntu/irtx-jsprit/java
mvn package
cd /home/ubuntu/irtx-jsprit

## Run verification
java -cp /home/ubuntu/irtx-jsprit/java/target/lead-jsprit-1.0.0.jar fr.irtx.lead.jsprit.RunVerification

## Run scenarios
for scenario in baseline_2022 ucc_2022 ucc_2030; do
	java -cp /home/ubuntu/irtx-jsprit/java/target/lead-jsprit-1.0.0.jar fr.irtx.lead.jsprit.RunSolver \
	  --problem-path /home/ubuntu/irtx-jsprit/output/scenario_${scenario}.json \
	  --solution-path /home/ubuntu/irtx-jsprit/output/solution_${scenario}.json \
	  --crs EPSG:2154 \
	  --osm-path /home/ubuntu/irtx-jsprit/output/scenario.osm.pbf \
	  --threads 12
done
