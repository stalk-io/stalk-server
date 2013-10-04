package io.stalk.common.oauth.utils;

import java.io.Serializable;

/**
 * Stores the BirthDate
 *
 * @author tarunn@brickred.com
 */
public class BirthDate implements Serializable {
    private static final long serialVersionUID = -3445329443489421113L;
    private int day;
    private int month;
    private int year;

    /**
     * Retrieves the birth day.
     *
     * @return the birth day
     */
    public int getDay() {
        return day;
    }

    /**
     * Updates the birth day
     *
     * @param day the birth day
     */
    public void setDay(final int day) {
        this.day = day;
    }

    /**
     * Retrieves the birth month
     *
     * @return the birth month
     */
    public int getMonth() {
        return month;
    }

    /**
     * Updates the birth month
     *
     * @param month the birth month
     */
    public void setMonth(final int month) {
        this.month = month;
    }

    /**
     * Retrieves the birth year
     *
     * @return the birth year
     */
    public int getYear() {
        return year;
    }

    /**
     * Updates the birth year
     *
     * @param year the birth year
     */
    public void setYear(final int year) {
        this.year = year;
    }

    /**
     * Returns the birth date in mm/dd/yyyy format
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (month > 0) {
            if (month < 10) {
                sb.append("0");
            }
            sb.append(month);
        } else {
            sb.append("00");
        }
        sb.append("/");
        if (day > 0) {
            if (day < 10) {
                sb.append("0");
            }
            sb.append(day);
        } else {
            sb.append("00");
        }
        sb.append("/");
        if (year > 0) {
            sb.append(year);
        } else {
            sb.append("0000");
        }
        return sb.toString();
    }
}
