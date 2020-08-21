package com.cyclingrecord.controllers;


import com.cyclingrecord.data.EntryRepository;
import com.cyclingrecord.data.YearTotalsRepository;
import com.cyclingrecord.models.Entry;
import com.cyclingrecord.models.YearTotals;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import javax.sound.midi.SysexMessage;
import javax.validation.Valid;
import java.sql.*;
import java.text.DateFormatSymbols;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.*;



@Controller
public class ChartController {

    @Autowired
    private EntryRepository entryRepository;

    @Autowired
    private YearTotalsRepository yearTotalsRepository;

    public ArrayList<LocalDate> getMonth() {
        ArrayList<LocalDate> entireMonth = new ArrayList<>();
        int getMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
        int getYear = Calendar.getInstance().get(Calendar.YEAR);
        YearMonth currentMonth = YearMonth.of(getYear, getMonth);
        for (int i = 1; i < currentMonth.lengthOfMonth() + 1; i++) {
            LocalDate ld = currentMonth.atDay(i);
            entireMonth.add(ld);
        }
        return entireMonth;
    }

    public ArrayList<String> getPreviousMonth(@RequestParam String date) {
        ArrayList<String> entireMonth = new ArrayList<>();
        String[] splitDate = date.split("-");
        int month = Integer.parseInt(splitDate[1].trim());
        int year = Integer.parseInt(splitDate[0]);
        YearMonth currentMonth = YearMonth.of(year, month);
        for (int i = 1; i < currentMonth.lengthOfMonth() + 1; i++) {
            LocalDate ld = currentMonth.atDay(i);
            entireMonth.add(ld.toString());
        }
        return entireMonth;
    }

    public String formatDate(LocalDate dateToFormat) {
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MMM");
        return dateToFormat.format(myFormatObj);
    }

    public ArrayList<String> formatMonth() {
        ArrayList<String> dateString = new ArrayList<>();
        for (int i = 0; i <= getMonth().size() - 1; i++) {
            dateString.add(formatDate(getMonth().get(i)));
        }
        return dateString;
    }

    public String formatPreviousDate(String dateToFormat) {
        LocalDate ld = LocalDate.parse(dateToFormat);
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MMM");
        String dayString = ld.format(myFormatObj);
        return dayString;
    }

    public ArrayList<String> formatPreviousMonth(String date) {
        ArrayList<String> dateString = new ArrayList<>();
        for (int i = 0; i <= getPreviousMonth(date).size() - 1; i++) {
            dateString.add(formatPreviousDate(getPreviousMonth(date).get(i)));
        }
        return dateString;
    }

    public static float sum(List<Float> list) {
        int sum = 0;
        for (float i : list) {
            sum += i;
        }
        return sum;
    }

    public ArrayList<String> getYearlyMonths() {
        ArrayList<String> monthsList = new ArrayList<>();
        String[] months = new DateFormatSymbols().getMonths();
        for (int i = 0; i < months.length; i++) {
            monthsList.add(months[i]);
        }
        return monthsList;
    }

    public ArrayList<String> getAbbrYearlyMonths() {
        ArrayList<String> monthsList = new ArrayList<>();
        String[] months = new DateFormatSymbols().getShortMonths();
        for (int i = 0; i < months.length; i++) {
            monthsList.add(months[i]);
        }
        return monthsList;
    }


    @RequestMapping(value="monthly", method = { RequestMethod.GET, RequestMethod.POST })
    public String showMonthlyTable(@ModelAttribute Entry entry, Model model, @RequestParam(required = false) String date, @RequestParam(required = false) Float distance, @RequestParam(required = false) Float time, @RequestParam(value="month", required = false) String month, WebRequest request) throws Exception {

        if (date == null || distance == null || time == null) {
            model.addAttribute("entries", entryRepository.findAll());
        } else {
            LocalDate localDate = LocalDate.parse(date);
            String formatDate = formatDate(localDate);

            Map<Integer, List<Float>> distanceByWeek = new HashMap<>();

            for (int i = 0; i < getPreviousMonth(date).size(); i++) {
                double speed = Math.round((distance / (time / 60)) * 100.0) / 100.0;
                Entry existingDate = entryRepository.findByDate(formatPreviousMonth(date).get(i));

                DayOfWeek dayOfWeek = LocalDate.parse(getPreviousMonth(date).get(i)).getDayOfWeek();
                int dayNumber = dayOfWeek.getValue();

                LocalDate weekday = LocalDate.parse(getPreviousMonth(date).get(i));
                int week = weekday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

                if (existingDate == null || !formatPreviousMonth(date).get(i).equals(existingDate.getDate())) {
                    Entry newEntry = new Entry();
                    newEntry.setDate(formatPreviousMonth(date).get(i));
                    newEntry.setDistance(0.0f);
                    newEntry.setTime(0.0f);
                    newEntry.setSpeed(0);
                    newEntry.setTotalDistance(0.0f);

                    entryRepository.save(newEntry);


                    if (formatPreviousMonth(date).get(i).equals(formatPreviousDate(date))) {
                        newEntry.setDistance(distance);
                        newEntry.setTime(time);
                        newEntry.setSpeed(speed);

                            List<Float> distances = new ArrayList<>();
                            distances.add(newEntry.getDistance());
                            for (Float listOfDistances : distances) {
                                distanceByWeek.computeIfAbsent(week, k -> new ArrayList<>()).add(listOfDistances);
                            }
                            Map<Integer, Integer> map = new HashMap<Integer, Integer>();
                            for (Map.Entry<Integer, List<Float>> totalDist : distanceByWeek.entrySet()) {
                                if (totalDist.getKey() == week && dayNumber == 7) {
                                    newEntry.setTotalDistance(sum(totalDist.getValue()));
                                }
                            }
                        entryRepository.save(newEntry);
                    }

                    List<Float> distances = new ArrayList<>();
                    distances.add(newEntry.getDistance());
                    for (Float listOfDistances : distances) {
                        distanceByWeek.computeIfAbsent(week, k -> new ArrayList<>()).add(listOfDistances);
                    }
                    for (Map.Entry<Integer, List<Float>> totalDist : distanceByWeek.entrySet()) {
                        if (totalDist.getKey() == week && dayNumber == 7) {
                            newEntry.setTotalDistance(sum(totalDist.getValue()));
                        }
                    }

                    entryRepository.save(newEntry);

                } else {
                    if (formatPreviousMonth(date).get(i).equals(formatPreviousDate(date))) {
                        existingDate.setTime(time);
                        existingDate.setDistance(distance);
                        existingDate.setSpeed(speed);

                        entryRepository.save(existingDate);
                    }

                    model.addAttribute("entries", entryRepository.findAll());

                }
                if (existingDate != null) {
                    List<Float> distances = new ArrayList<>();
                    distances.add(existingDate.getDistance());
                    for (Float listOfDistances : distances) {
                        distanceByWeek.computeIfAbsent(week, k -> new ArrayList<>()).add(listOfDistances);
                    }
                    for (Map.Entry<Integer, List<Float>> totalDist : distanceByWeek.entrySet()) {
                        if (totalDist.getKey() == week && dayNumber == 7) {
                            existingDate.setTotalDistance(sum(totalDist.getValue()));
                        }
                    }
                    entryRepository.save(existingDate);
                }
            }
        }
        ArrayList<String> monthAbbrList = new ArrayList<>();
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/cyclingrecord", "cyclingrecord", "Hmveonl00");
        //Get array of month abbreviations
        PreparedStatement monthStmt = conn.prepareStatement("SELECT month_abbr FROM cyclingrecord.year_totals");
        ResultSet monthResultSet = monthStmt.executeQuery();
        while (monthResultSet.next()) {
            monthAbbrList.add(monthResultSet.getString("month_abbr"));
        }




        model.addAttribute("months", monthAbbrList);

        System.out.println(request.getParameter("month"));
        //Set monthly view

        ArrayList<String> rd = new ArrayList<>();

        PreparedStatement dayStmt = conn.prepareStatement("SELECT * FROM cyclingrecord.entry WHERE date LIKE CONCAT('%',?,'%')");
        dayStmt.setString(1, request.getParameter("month"));
        ResultSet dayResultSet = dayStmt.executeQuery();

        while(dayResultSet.next()){
            String resultDate = dayResultSet.getString("date");
            rd.add(resultDate);
            model.addAttribute("monthResult", rd);
        }

        model.addAttribute("entries", entryRepository.findAll());
        return "monthly";
    }
/*
    @PostMapping("/monthly")
    public String montlySubmittal(Model model,  @RequestParam(required=false) String month) throws SQLException {
        System.out.println(month);
        //Set monthly view
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/cyclingrecord", "cyclingrecord", "Hmveonl00");
        ArrayList<String> rd = new ArrayList<>();

        PreparedStatement dayStmt = conn.prepareStatement("SELECT * FROM cyclingrecord.entry WHERE date LIKE CONCAT('%',?,'%')");
        dayStmt.setString(1, month);
        ResultSet dayResultSet = dayStmt.executeQuery();

        while(dayResultSet.next()){
            String resultDate = dayResultSet.getString("date");
            rd.add(resultDate);
            model.addAttribute("monthResult", rd);
        }

        ArrayList<String> monthAbbrList = new ArrayList<>();

        //Get array of month abbreviations
        PreparedStatement monthStmt = conn.prepareStatement("SELECT month_abbr FROM cyclingrecord.year_totals");
        ResultSet monthResultSet = monthStmt.executeQuery();
        while (monthResultSet.next()) {
            monthAbbrList.add(monthResultSet.getString("month_abbr"));
        }




        model.addAttribute("months", monthAbbrList);
        return "monthly";
    }
*/
    @RequestMapping("/yearly")
    public String yearTotalChart(Model model) throws SQLException {
        int year = Year.now().getValue();
        Integer yearNumber = 0;

        if (!yearTotalsRepository.existsByYear(year)) {
            for (int i = 0; i < 12; i++) {

                YearTotals yearTotals = new YearTotals();

                yearTotals.setMonth(getYearlyMonths().get(i));
                yearTotals.setYear(year);
                yearTotals.setTotal(0.0f);
                yearTotals.setGrandTotal(0);
                yearTotals.setMonthAbbr(getAbbrYearlyMonths().get(i));

                yearTotalsRepository.save(yearTotals);
            }
        } else {

            ArrayList<Integer> multipleYearNums = new ArrayList<>();
            Map<String, Float> distanceByMonth = new HashMap<>();


            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/cyclingrecord", "cyclingrecord", "Hmveonl00");

            PreparedStatement yearStmt = conn.prepareStatement("SELECT year FROM cyclingrecord.year_totals");
            ResultSet yearResultSet = yearStmt.executeQuery();
            while (yearResultSet.next()) {
                multipleYearNums.add(yearResultSet.getInt("year"));
            }

            ArrayList<String> monthAbbrList = new ArrayList<>();
            ArrayList<Float> monthTotals = new ArrayList<>();
            ArrayList<String> dateCombinationByMonth = new ArrayList<>();

            //Get array of month abbreviations
            PreparedStatement monthStmt = conn.prepareStatement("SELECT month_abbr FROM cyclingrecord.year_totals");
            ResultSet monthResultSet = monthStmt.executeQuery();
            while (monthResultSet.next()) {
                monthAbbrList.add(monthResultSet.getString("month_abbr"));
            }

            //Get all days
            PreparedStatement dayStmt = conn.prepareStatement("SELECT date FROM cyclingrecord.entry");
            ResultSet dayResultSet = dayStmt.executeQuery();
            while (dayResultSet.next()) {
                dateCombinationByMonth.add(dayResultSet.getString("date"));
            }

            //Get all distances
            PreparedStatement distanceStmt = conn.prepareStatement("SELECT distance FROM cyclingrecord.entry");
            ResultSet distanceResultSet = distanceStmt.executeQuery();
            while (distanceResultSet.next()) {
                monthTotals.add(distanceResultSet.getFloat("distance"));
            }

                for (int j = 0; j < dateCombinationByMonth.size(); j++) {
                    Iterable<YearTotals> existingYearTotals = yearTotalsRepository.findAll();
                    String day = dateCombinationByMonth.get(j);
                    Float monthlyDistance = monthTotals.get(j);

                distanceByMonth.put(day, monthlyDistance);
            }
            Map<String, Float> result = new HashMap<>();
            for(Map.Entry<String, Float> distByMonth : distanceByMonth.entrySet()){
                String key = distByMonth.getKey().split("-")[1];
                Float value = distByMonth.getValue();
                Float oldValue = result.get(key) != null ? result.get(key) : 0.0f;
                result.put(key, oldValue + value);
            }

            Iterable<YearTotals> existingYearTotals = yearTotalsRepository.findAll();
            for (YearTotals exYearTotals : existingYearTotals) {
                for (String entry : result.keySet()) {
                    for(int k=0; k<12; k++){
                        if (entry.equals(exYearTotals.getMonthAbbr())) {
                            exYearTotals.setTotal(result.get(entry));
                        }
                    }

                }
                exYearTotals.setYear(year);
                exYearTotals.setGrandTotal(0);
                yearTotalsRepository.save(exYearTotals);
            }

            if (multipleYearNums.contains(year)) {
                yearNumber = year;
            }

            model.addAttribute("yearNumber", yearNumber);
            model.addAttribute("yearTotals", yearTotalsRepository.findAll());
            conn.close();
        }
        return "yearly";
    }
}







