import sys
import json

### Parse arguments
arguments = sys.argv[1:]

scenario_path = None
output_path = None
operator_paths = []

shipment_types = {}
consolidation_types = {}
salaries = {}

vehicle_type_properties = []

k = 0
while k < len(arguments):
    if arguments[k] == "--scenario-path":
        k += 1

        if k == len(arguments):
            raise RuntimeError("Need to provide path for scenario")

        scenario_path = arguments[k]
        print("Scenario:", scenario_path)

    elif arguments[k] == "--output-path":
        k += 1

        if k == len(arguments):
            raise RuntimeError("Need to provide output path")

        output_path = arguments[k]
        print("Output:", output_path)

    elif arguments[k] == "--operator-path":
        k += 1

        if k == len(arguments):
            raise RuntimeError("Need to provide operator path")

        operator_paths.append(arguments[k])
        print("Operator:", operator_paths[-1])

    elif arguments[k].startswith("--shipment-type:"):
        operator = arguments[k].split(":")[1]
        k += 1

        if k == len(arguments):
            raise RuntimeError("Need to provide shipment type for {}".format(operator))

        shipment_type = arguments[k]

        if not shipment_type in ("delivery", "pickup", "none"):
            raise RuntimeError("Invalid shipment type for {}".format(operator))

        shipment_types[operator] = shipment_type
        print("Shipment type", shipment_type, "for", operator)

    elif arguments[k].startswith("--consolidation-type:"):
        operator = arguments[k].split(":")[1]
        k += 1

        if k == len(arguments):
            raise RuntimeError("Need to provide consolidation type for {}".format(operator))

        consolidation_type = arguments[k]

        if not consolidation_type in ("delivery", "pickup", "none"):
            raise RuntimeError("Invalid consolidation type for {}".format(operator))

        consolidation_types[operator] = consolidation_type
        print("Consolidation type", consolidation_type, "for", operator)

    elif arguments[k].startswith("--driver-salary:"):
        operator = arguments[k].split(":")[1]
        k += 1

        if k == len(arguments):
            raise RuntimeError("Need to provide driver salary for {}".format(operator))

        salary = float(arguments[k])
        salaries[operator] = salary
        print("Salary ", salary, "EUR for", operator)

    elif arguments[k].startswith("--vehicle-type:"):
        vehicle_type, property = arguments[k].split(":")[1:]
        k += 1

        if k == len(arguments):
            raise RuntimeError("Need to provide value for property {} of vehicle type{}".format(property, vehicle_type))

        if property in ("cost_per_day_EUR", "cost_per_km_EUR", "co2_per_km_g", "energy_per_km_Wh", "speed_km_h"):
            vehicle_type_properties.append((vehicle_type, property, float(arguments[k])))

        elif property in ("capacity"):
            vehicle_type_properties.append((vehicle_type, property, int(arguments[k])))
        else:
            raise RuntimeError("Unknown vehicle type property:", property)

    else:
        raise RuntimeError("Don't understand argument: {}".format(arguments[k]))

    k += 1

if scenario_path is None:
    raise RuntimeError("No scenario path (--scenario-path) specified")

if output_path is None:
    raise RuntimeError("No output path (--output-path) specified")

### Load scenario$
with open(scenario_path, encoding="utf8") as f:
    scenario = json.load(f)

### Integrate operators
if not "operators" in scenario:
    scenario["operators"] = []

operator_ids = {operator["id"] for operator in scenario["operators"]}

for operator_path in operator_paths:
    with open(operator_path, encoding="utf8") as f:
        operator = json.load(f)

    if operator["id"] in operator_ids:
        raise RuntimeError("Operator {} from {} is already defined".format(
            operator["id"], operator_path
        ))

    scenario["operators"].append(operator)
    operator_ids.add(operator["id"])

### Update shipment and consolidation types

for operator_id, shipment_type in shipment_types.items():
    if not operator_id in operator_ids:
        raise RuntimeError("Shipment type given for non-existing operator", operator)

    for operator in scenario["operators"]:
        if operator["id"] == operator_id:
            operator["shipment_type"] = shipment_type

for operator_id, consolidation_type in consolidation_types.items():
    if not operator_id in operator_ids:
        raise RuntimeError("Consolidation type given for non-existing operator", operator)

    for operator in scenario["operators"]:
        if operator["id"] == operator_id:
            operator["consolidation_type"] = consolidation_type

for operator_id, salary in salaries.items():
    if not operator_id in operator_ids:
        raise RuntimeError("Salary given for non-existing operator", operator)

    for operator in scenario["operators"]:
        if operator["id"] == operator_id:
            operator["daily_driver_salary_EUR"] = salary

### Update vehicle type properties
vehicle_type_ids = {vt["id"] for vt in scenario["vehicle_types"]}

for vehicle_type_id, property, value in vehicle_type_properties:
    if not vehicle_type_id in vehicle_type_ids:
        raise RuntimeError("Vehicle type does not exist:", vehicle_type_id)

    for vehicle_type in scenario["vehicle_types"]:
        if vehicle_type["id"] == vehicle_type_id:
            vehicle_type[property] = value

### Output
with open(output_path, "w+", encoding='utf8') as f:
    json.dump(scenario, f)
