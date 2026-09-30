// Mirrors com.aeroops.flights.FlightView on the API side.
export interface FlightView {
  id: string;
  carrier: string;
  flightNumber: string;
  serviceDate: string;
  origin: string;
  destination: string;
  scheduledOnBlock: string | null;
  estimatedOnBlock: string | null;
  actualOnBlock: string | null;
  scheduledOffBlock: string | null;
  estimatedOffBlock: string | null;
  actualOffBlock: string | null;
  lifecycle: string;
  sourceSystem: string;
  updatedAt: string;
}
