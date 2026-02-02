#!/usr/bin/env python3

from http.server import HTTPServer, BaseHTTPRequestHandler
import json
import sys
from urllib.parse import urlparse
import math
import datetime
import traceback


class MockTimeFilterHandler(BaseHTTPRequestHandler):
    log_file = open('server_connections.log', 'a')

    def do_POST(self):
        if self.path == '/v4/time-filter':
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            request = json.loads(post_data.decode('utf-8'))

            try:
                response = self.generate_response(request)
            except Exception:
                self.log_message("Error generating response: %s", traceback.format_exc())
                self.send_response(500)
                self.end_headers()
                return

            self.log_message("Generated response for request")
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps(response).encode('utf-8'))
        else:
            self.send_response(404)
            self.end_headers()

    def process_search(self, search, locations_map, origin_key, target_key):
        search_id = search['id']
        origin_location = locations_map[search["%s_location_id" % origin_key]]

        self.log_message("Properties requested: %s", search['properties'])

        locations = []
        unreachable = []
        self.log_message("Processing search %s with %d targets", search_id, len(search["%s_location_ids" % target_key]))

        for target_id in search["%s_location_ids" % target_key]:
            target_location = locations_map[target_id]
            distance = self.calculate_distance(
                origin_location['coords']['lat'],
                origin_location['coords']['lng'],
                target_location['coords']['lat'],
                target_location['coords']['lng']
            )
            # Rough estimate: 50 meters per second average travel time
            travel_time = int(distance / 50)

            if travel_time <= search['travel_time']:
                properties = []
                if 'travel_time' in search['properties']:
                    properties.append({'travel_time': travel_time})
                if 'distance' in search['properties']:
                    properties.append({'distance': int(distance)})

                locations.append({
                    'id': target_id,
                    'properties': properties
                })
            else:
                unreachable.append(target_id)

        return {
            'search_id': search_id,
            'locations': locations,
            'unreachable': unreachable
        }

    def generate_response(self, request):
        results = []
        locations_map = {}
        for loc in request['locations']:
            locations_map[loc['id']] = loc

        # Process arrival searches
        if 'arrival_searches' in request:
            for search in request['arrival_searches']:
                res = self.process_search(search, locations_map, 'arrival', 'departure')
                results.append(res)

        # Process departure searches
        if 'departure_searches' in request:
            for search in request['departure_searches']:
                res = self.process_search(search, locations_map, 'departure', 'arrival')
                results.append(res)

        return {'results': results}

    def calculate_distance(self, lat1, lon1, lat2, lon2):
        # Haversine formula for distance in meters
        R = 6371000  # Earth radius in meters
        phi1 = math.radians(lat1)
        phi2 = math.radians(lat2)
        delta_phi = math.radians(lat2 - lat1)
        delta_lambda = math.radians(lon2 - lon1)

        a = math.sin(delta_phi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))

        return R * c

    def log_message(self, format, *args):
        timestamp = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        self.log_file.write("{} - {} - {}\n".format(timestamp, self.client_address[0], format % args))
        self.log_file.flush()


if __name__ == '__main__':
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    server = HTTPServer(('0.0.0.0', port), MockTimeFilterHandler)
    print('Mock TimeFilter server listening on port {}'.format(port))
    sys.stdout.flush()
    server.serve_forever()
