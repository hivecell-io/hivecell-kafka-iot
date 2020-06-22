package com.rickerlyman.iot.sense.core.math;

import java.text.DecimalFormat;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Formulas {
    private static final double CELSIUS_OFFSET = 273.15;

    private static final double c1 = -42.379;
    private static final double c2 = 2.04901523;
    private static final double c3 = 10.14333127;
    private static final double c4 = 0.22475541;
    private static final double c5 = 6.83783E-3;
    private static final double c6 = 5.481717E-2;
    private static final double c7 = 1.22874E-3;
    private static final double c8 = 8.5282E-4;
    private static final double c9 = 1.99E-6;

    private static final double E = 2.718281828;

    public static double heatIndex(double temperature, double relativeHumidity) {
        temperature = celsiusToFahrenheit(temperature);
        double t2 = temperature * temperature;
        double r2 = relativeHumidity * relativeHumidity;
        double hIndex;
        hIndex = c1 + c2 * temperature + c3 * relativeHumidity;
        hIndex = hIndex - c4 * temperature * relativeHumidity;
        hIndex = hIndex - c5 * t2;
        hIndex = hIndex - c6 * r2;
        hIndex = hIndex + c7 * t2 * relativeHumidity;
        hIndex = hIndex + c8 * temperature * r2;
        hIndex = hIndex - c9 * t2 * r2;

        return fahrenheitToCelsius(hIndex);
    }

    public static double humIndexByTempDP(double temperature, double relativeHumidity) {
        double pow = 5417.7530 * ((1 / 273.16) - (1 / celsiusToKelvin(dewPoint(temperature, relativeHumidity))));
        return temperature + 0.5555 * (6.11 * Math.pow(E, pow) - 10);
    }

    public static double humIndexByTempRH(double temperature, double relativeHumidity) {
        double pow = 7.5 * (temperature / (237.7 + temperature));
        return temperature + (5f / 9) * (6.112 * Math.pow(10, pow) * relativeHumidity / 100 - 10);
    }

    public static double summerSimmerIndex(double temperature, double relativeHumidity) {
        return 1.98 * (temperature - (0.55 - 0.0055 * relativeHumidity) * (temperature - 58)) - 56.83;
    }

    public static double dewPoint(double temperature, double relativeHumidity) {
        return (temperature - (14.55 + 0.114 * temperature) * (1 - (0.01 * relativeHumidity)) - Math.pow(((2.5 + 0.007 * temperature) * (1 - (0.01 * relativeHumidity))), 3) - (15.9 + 0.117 * temperature) * Math.pow((1 - (0.01 * relativeHumidity)), 14));
    }

    public static float truncatePrecision(double num, int precision) {
        String pattern = "#.";
        String n = IntStream.generate(() -> 0).limit(precision).boxed().map(Object::toString).collect(Collectors.joining(""));
        DecimalFormat df = new DecimalFormat(pattern + n);
        return Float.valueOf(df.format(num));
    }

    public static float truncatePrecision(float num, int precision) {
        String pattern = "#.";
        String n = IntStream.generate(() -> 0).limit(precision).boxed().map(Object::toString).collect(Collectors.joining(""));
        DecimalFormat df = new DecimalFormat(pattern + n);
        return Float.valueOf(df.format(num));
    }

    private static double celsiusToFahrenheit(double temp) {
        return temp * (9f / 5) + 32;
    }

    private static double celsiusToKelvin(double temp) {
        return temp + CELSIUS_OFFSET;
    }

    private static double fahrenheitToCelsius(double temp) {
        return (temp - 32) * 5f / 9;
    }


}
