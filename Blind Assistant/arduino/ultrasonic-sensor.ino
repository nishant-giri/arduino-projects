// Ultrasonic Sensor - Nishant Giri

const int trigPin = 12;
const int echoPin = 13;
void setup() 
{
  // Setup Code to Run Once
  Serial.begin(9600);
  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);
}
void loop() 
{
  // Main Code to Run Repeatedly
  long duration, cm;  
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  // Distance Calculation
  duration = pulseIn(echoPin, HIGH);
  cm = microsecondsToCentimeters(duration);
  Serial.println(cm);
  delay(50);
}
long microsecondsToInches(long microseconds)
{
  return ((microseconds / 74) / 2);
}
long microsecondsToCentimeters(long microseconds)
{
  return ((microseconds / 29) / 2);
}