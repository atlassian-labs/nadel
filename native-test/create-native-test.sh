cd ..
./gradlew build
CP=`./gradlew printTestClasspath -q`:../build/classes/java/main:../build/classes/java/test
cd native-test
$GRAALVM_HOME/bin/native-image --verbose --no-fallback -H:+ReportExceptionStackTraces -H:ReflectionConfigurationFiles=reflect.json -cp $CP benchmark.LargeResponseBenchmarkNative native-benchmark
chmod +x native-benchmark
echo "RUN the test with ./native-benchmark"