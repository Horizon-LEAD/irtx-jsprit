#!/bin/bash

python3 --version
which python
java -cp /srv/app/jsprit/java/target/lead-jsprit-1.0.0.jar fr.irtx.lead.jsprit.RunVerification

#Set fonts
NORM=`tput sgr0`
BOLD=`tput bold`
REV=`tput smso`

function show_usage () {
    echo -e "${BOLD}Basic usage:${NORM} entrypoint.sh [-vh] perimeter osm operator shipment_type consolidation_type output-path"
}

function show_help () {
    echo -e "${BOLD}entrypoint.sh${NORM}: Calls the entrypoint"\\n
    show_usage
    echo -e "\n${BOLD}Required arguments:${NORM}"
    echo -e "${REV}perimeter${NORM}\t\t the path of the perimeter gpkg file"
    echo -e "${REV}osm${NORM}\t\t the path of the osm file"
    echo -e "${REV}operator${NORM}\t\t the path of the operator json file"
    echo -e "${REV}shipment_type${NORM}\t\t str values {delivery, pickup, none}"
    echo -e "${REV}consolidation_type${NORM}\t\t str values {delivery, pickup, none}"
    echo -e "${REV}output-path{NORM}\t\t\t the output directory"\\n
    echo -e "${BOLD}Optional arguments:${NORM}"
    echo -e "${REV}-v${NORM}\tSets verbosity level"
    echo -e "${REV}-h${NORM}\tShows this message"
    echo -e "${BOLD}Examples:${NORM}"
    echo -e "./entrypoint.sh -v ./sample-data/input/perimeter_lyon.gpkg ./sample-data/input/rhone-alpes-latest.osm.pbf ./sample-data/output/"
}

####################################################################################################
# GETOPTS                                                                                          #
####################################################################################################
# A POSIX variable
# Reset in case getopts has been used previously in the shell.
OPTIND=1

# Initialize vars:
verbose=0

# while getopts
while getopts 'hv' OPTION; do
    case "$OPTION" in
        h)
            show_help
            kill -INT $$
            ;;
        v)
            verbose=1
            ;;
        ?)
            show_usage >&2
            kill -INT $$
            ;;
    esac
done

shift "$(($OPTIND -1))"

leftovers=(${@})
fperimeter=${leftovers[0]}
fosm=${leftovers[1]}
foperator=${leftovers[2]}
shipment_type=${leftovers[3]}
consolidation_type=${leftovers[4]}
outdir=${leftovers[5]%/}

####################################################################################################
# Input checks                                                                                     #
####################################################################################################
if [ ! -f "${fperimeter}" ]; then
     echo -e "Give a ${BOLD}valid${NORM} input perimeter file path\n"; show_usage; kill -INT $$
fi

if [ ! -d "${outdir}" ]; then
     echo -e "Give a ${BOLD}valid${NORM} output directory\n"; show_usage; kill -INT $$
fi

####################################################################################################
# Execution                                                                                        #
####################################################################################################

python3 /srv/app/jsprit/prepare_perimeter.py \
  --input-path ${fperimeter} \
  --output-path ${outdir}/perimeter_lyon.poly

sh /srv/app/jsprit/prepare_osm.sh \
  ${fosm} \
  ${outdir}/perimeter_lyon.poly \
  ${outdir}/scenario.osm.pbf

operator_id=$(cat ${foperator} | jq .id | tr -d '"')

python3 /srv/app/jsprit/prepare_scenario.py \
  --scenario-path /srv/app/data/template_lyon.json \
  --output-path ${outdir}/scenario.json \
  --operator-path ${foperator} \
  --shipment-type:${operator_id} ${shipment_type} \
  --consolidation-type:${operator_id} ${consolidation_type}

# Run verification
java -cp /srv/app/jsprit/java/target/lead-jsprit-1.0.0.jar fr.irtx.lead.jsprit.RunVerification

# Run scenario
java -cp /srv/app/jsprit/java/target/lead-jsprit-1.0.0.jar fr.irtx.lead.jsprit.RunSolver \
  --problem-path ${outdir}/scenario.json \
  --solution-path ${outdir}/solution.json \
  --crs EPSG:2154 \
  --osm-path ${outdir}/scenario.osm.pbf \
  --threads 4
