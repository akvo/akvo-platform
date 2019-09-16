import argparse
import sys
from google.cloud import datastore

def kind_run_query(client):
    query = client.query(kind='__kind__')
    query.keys_only()

    kinds = [entity.key.id_or_name for entity in query.fetch()]

    return kinds

client = datastore.Client(project=sys.argv[1])

all_kinds = kind_run_query(client)
result = filter(lambda x: not x.startswith("_"), all_kinds)
result = [x for x in result if x not in ['EventQueue', 'ColumnContainer', 'AccessPointMetricSummary', 'AccessPointMetricMapping', 'AccessPointMappingHistory', 'MappingSpreadsheetDefinition', 'MapFragment', 'MapControl', 'GeoRegion', 'StandardScoring', 'Standard', 'SpreadsheetContainer', 'RowContainer', 'QuestionHelpMedia', 'WebActivityAuthorization', 'UnitOfMeasure', 'TechnologyType']]
print(','.join(result))