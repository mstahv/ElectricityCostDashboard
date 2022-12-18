package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.Ping;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolPrice;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolResponse;
import com.vesanieminen.froniusvisualizer.util.css.Background;
import com.vesanieminen.froniusvisualizer.util.css.BorderColor;
import com.vesanieminen.froniusvisualizer.util.css.FontFamily;
import com.vesanieminen.froniusvisualizer.util.css.Layout;
import com.vesanieminen.froniusvisualizer.util.css.Transform;
import com.vesanieminen.froniusvisualizer.util.css.Transition;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.vesanieminen.froniusvisualizer.services.NordpoolSpotService.getLatest7Days;
import static com.vesanieminen.froniusvisualizer.services.NordpoolSpotService.getLatest7DaysList;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceThisMonth;
import static com.vesanieminen.froniusvisualizer.util.Utils.convertNordpoolLocalDateTimeToFinnish;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentLocalDateTimeHourPrecisionFinnishZone;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentTimeWithHourPrecision;
import static com.vesanieminen.froniusvisualizer.util.Utils.getVAT;
import static com.vesanieminen.froniusvisualizer.util.Utils.threeDecimals;
import static com.vesanieminen.froniusvisualizer.util.Utils.vat10Instant;
import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@PageTitle("List" + URL_SUFFIX)
@Route(value = "lista", layout = MainLayout.class)
@RouteAlias(value = "hintalista", layout = MainLayout.class)
@RouteAlias(value = "price-list", layout = MainLayout.class)
public class PriceListView extends Main {

    private final double expensiveLimit;
    private final double cheapLimit;

    public PriceListView() {
        addClassNames(LumoUtility.Overflow.AUTO, LumoUtility.Padding.Horizontal.SMALL);
        // Set height to correctly position sticky dates
        // Added fix for iOS Safari header height that changes when scrolling
        setHeight("var(--fullscreen-height)");
        expensiveLimit = calculateSpotAveragePriceThisMonth();
        cheapLimit = expensiveLimit / 2;
    }

    private static double getPrice(NumberFormat format, NordpoolResponse.Column column, LocalDateTime localDateTime) throws ParseException {
        double price;
        if (0 < localDateTime.compareTo(vat10Instant.atZone(fiZoneID).toLocalDateTime())) {
            price = format.parse(column.Value).doubleValue() * 1.10 / 10;
        } else {
            price = format.parse(column.Value).doubleValue() * 1.24 / 10;
        }
        return price;
    }

    private static NordpoolResponse getData() {
        return getLatest7Days();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        //renderView(attachEvent);
        renderListView(attachEvent);
    }

    void renderView(AttachEvent attachEvent) {
        var data = getData();
        if (data == null) {
            return;
        }
        addRows(data, attachEvent);
    }

    private void addRows(NordpoolResponse data, AttachEvent attachEvent) {
        var locale = attachEvent.getUI().getLocale();
        Collection<Component> containerList = new ArrayList<>();
        ListItem currentItem = null;
        var now = getCurrentTimeWithHourPrecision();
        NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        final var rows = data.data.Rows;
        int columnIndex = 6;
        while (columnIndex >= 0) {
            final var day = new H2();
            day.addClassNames(
                    LumoUtility.Background.BASE,
                    LumoUtility.Border.BOTTOM,
                    LumoUtility.BorderColor.CONTRAST_10,
                    LumoUtility.FontSize.XXLARGE,
                    Layout.TOP_0,
                    LumoUtility.Margin.Bottom.NONE,
                    LumoUtility.Margin.Horizontal.AUTO,
                    LumoUtility.Margin.Top.MEDIUM,
                    LumoUtility.MaxWidth.SCREEN_SMALL,
                    LumoUtility.Padding.SMALL,
                    LumoUtility.Position.STICKY,
                    Layout.Z_10
            );
            containerList.add(day);

            final var list = new UnorderedList();
            list.addClassNames(
                    FontFamily.MONO,
                    LumoUtility.ListStyleType.NONE,
                    LumoUtility.Margin.Horizontal.AUTO,
                    LumoUtility.Margin.Vertical.NONE,
                    LumoUtility.MaxWidth.SCREEN_SMALL,
                    LumoUtility.Padding.NONE
            );
            containerList.add(list);

            for (NordpoolResponse.Row row : rows.subList(0, rows.size() - 6)) {
                final var time = row.StartTime.toString().split("T")[1];
                NordpoolResponse.Column column = row.Columns.get(columnIndex);
                final var dateTimeString = column.Name + " " + time;
                final var dataLocalDataTime = LocalDateTime.parse(dateTimeString, dateTimeFormatter);
                // Convert the Nordpool timezone (Norwegian) to Finnish time zone
                final var localDateTime = convertNordpoolLocalDateTimeToFinnish(dataLocalDataTime);
                day.setText(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(dataLocalDataTime));
                try {
                    final var price = getPrice(format, column, localDateTime);
                    final var timeSpan = new Span(DateTimeFormatter.ofPattern("HH:mm").withLocale(locale).format(localDateTime));
                    final var df = new DecimalFormat("#0.000");
                    final var priceSpan = new Span(df.format(price) + "¢");

                    final var item = new ListItem(timeSpan, priceSpan);
                    item.addClassNames(
                            LumoUtility.Border.BOTTOM,
                            LumoUtility.Display.FLEX,
                            LumoUtility.JustifyContent.BETWEEN,
                            LumoUtility.Padding.SMALL,
                            LumoUtility.FontSize.LARGE
                    );

                    setPriceTextColor(price, priceSpan);

                    // Add the hover effect only for desktop browsers
                    attachEvent.getUI().getPage().retrieveExtendedClientDetails(details -> {
                        if (!details.isTouchDevice()) {
                            item.addClassNames(
                                    Transform.Hover.SCALE_102,
                                    Transition.TRANSITION
                            );
                            if (price <= cheapLimit) {
                                item.addClassNames(Background.Hover.SUCCESS_10, BorderColor.Hover.SUCCESS);
                            }
                            if (price > cheapLimit && price < expensiveLimit) {
                                item.addClassNames(Background.Hover.PRIMARY_10, BorderColor.Hover.PRIMARY);
                            }
                            if (price >= expensiveLimit) {
                                item.addClassNames(Background.Hover.ERROR_10, BorderColor.Hover.ERROR);
                            }
                        }
                    });


                    // Current item
                    if (Objects.equals(localDateTime, now)) {
                        final var current = getTranslation("Current");
                        Ping ping = new Ping(current, getPriceBackgroundColor(price));

                        timeSpan.setText(timeSpan.getText() + " ");
                        timeSpan.add(ping);
                        setPriceTextColor(price, timeSpan);

                        item.addClassNames(
                                getPriceBackgroundColor_10(price),
                                getPriceBorderColor(price),
                                LumoUtility.FontWeight.BOLD
                        );
                    } else {
                        item.addClassNames(LumoUtility.BorderColor.CONTRAST_10);
                    }
                    list.add(item);

                    // Due to the sticky position of some elements we need to scroll to the position of -2h
                    if (Objects.equals(localDateTime, now.minusHours(2))) {
                        currentItem = item;
                    }
                } catch (ParseException e) {
                }
            }
            --columnIndex;
        }

        add(containerList);
        currentItem.scrollIntoView();
    }

    private void setPriceTextColor(double price, Span span) {
        if (price <= cheapLimit) {
            span.addClassName(LumoUtility.TextColor.SUCCESS);
        }
        if (price > cheapLimit && price < expensiveLimit) {
            span.addClassName(LumoUtility.TextColor.PRIMARY);
        }
        if (price >= expensiveLimit) {
            span.addClassName(LumoUtility.TextColor.ERROR);
        }
    }

    private String getPriceBackgroundColor(double price) {
        if (price <= cheapLimit) {
            return LumoUtility.Background.SUCCESS;
        }
        if (price < expensiveLimit) {
            return LumoUtility.Background.PRIMARY;
        }
        if (price >= expensiveLimit) {
            return LumoUtility.Background.ERROR;
        }
        return LumoUtility.Background.PRIMARY;
    }

    private String getPriceBackgroundColor_10(double price) {
        if (price <= cheapLimit) {
            return LumoUtility.Background.SUCCESS_10;
        }
        if (price < expensiveLimit) {
            return LumoUtility.Background.PRIMARY_10;
        }
        if (price >= expensiveLimit) {
            return LumoUtility.Background.ERROR_10;
        }
        return LumoUtility.Background.PRIMARY_10;
    }

    private String getPriceBorderColor(double price) {
        if (price <= cheapLimit) {
            return LumoUtility.BorderColor.SUCCESS;
        }
        if (price < expensiveLimit) {
            return LumoUtility.BorderColor.PRIMARY;

        }
        if (price >= expensiveLimit) {
            return LumoUtility.BorderColor.ERROR;
        }
        return LumoUtility.BorderColor.PRIMARY;
    }

    void renderListView(AttachEvent attachEvent) {
        var data = getLatest7DaysList();
        if (data.isEmpty()) {
            return;
        }
        addRows(data, attachEvent);
    }

    private void addRows(List<NordpoolPrice> data, AttachEvent attachEvent) {
        var locale = attachEvent.getUI().getLocale();
        Collection<Component> containerList = new ArrayList<>();
        ListItem currentItem = null;
        final var nowLocalDateTime = getCurrentLocalDateTimeHourPrecisionFinnishZone();
        H2 day = null;
        UnorderedList list = null;
        Span daySpan;
        var currentDayTime = data.get(0).time();
        boolean first = true;
        for (NordpoolPrice price : data) {
            var currentDay = price.time();
            if (currentDay.atZone(fiZoneID).truncatedTo(ChronoUnit.DAYS).isAfter(currentDayTime.atZone(fiZoneID).truncatedTo(ChronoUnit.DAYS)) || first) {
                first = false;
                currentDayTime = currentDay;
                day = createDayH2();
                containerList.add(day);
                list = createUnorderedList();
                containerList.add(list);
                daySpan = createDaySpan();
                daySpan.setText(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(price.time().atZone(fiZoneID)));
            }
            final var localDateTime = price.time().atZone(fiZoneID).toLocalDateTime();
            day.setText(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(localDateTime));
            final var timeSpan = new Span(DateTimeFormatter.ofPattern("HH:mm").withLocale(locale).format(localDateTime));
            final var vatPrice = price.price() * getVAT(price.time());
            final var priceSpan = new Span(threeDecimals.format(vatPrice) + "¢");
            final ListItem item = createListItem(timeSpan, priceSpan);

            setPriceTextColor(vatPrice, priceSpan);

            // Add the hover effect only for desktop browsers
            attachEvent.getUI().getPage().retrieveExtendedClientDetails(details -> {
                if (!details.isTouchDevice()) {
                    item.addClassNames(
                            Transform.Hover.SCALE_102,
                            Transition.TRANSITION
                    );
                    if (vatPrice <= cheapLimit) {
                        item.addClassNames(Background.Hover.SUCCESS_10, BorderColor.Hover.SUCCESS);
                    }
                    if (vatPrice > cheapLimit && vatPrice < expensiveLimit) {
                        item.addClassNames(Background.Hover.PRIMARY_10, BorderColor.Hover.PRIMARY);
                    }
                    if (vatPrice >= expensiveLimit) {
                        item.addClassNames(Background.Hover.ERROR_10, BorderColor.Hover.ERROR);
                    }
                }
            });


            // Current item
            if (Objects.equals(localDateTime, nowLocalDateTime)) {
                final var current = getTranslation("Current");
                Ping ping = new Ping(current, getPriceBackgroundColor(vatPrice));

                timeSpan.setText(timeSpan.getText() + " ");
                timeSpan.add(ping);
                setPriceTextColor(vatPrice, timeSpan);

                item.addClassNames(
                        getPriceBackgroundColor_10(vatPrice),
                        getPriceBorderColor(vatPrice),
                        LumoUtility.FontWeight.BOLD
                );
            } else {
                item.addClassNames(LumoUtility.BorderColor.CONTRAST_10);
            }
            list.add(item);

            // Due to the sticky position of some elements we need to scroll to the position of -2h
            if (Objects.equals(localDateTime, nowLocalDateTime.minusHours(2))) {
                currentItem = item;
            }
        }
        add(containerList);
        currentItem.scrollIntoView();
    }

    private static ListItem createListItem(Span timeSpan, Span priceSpan) {
        final var item = new ListItem(timeSpan, priceSpan);
        item.addClassNames(
                LumoUtility.Border.BOTTOM,
                LumoUtility.Display.FLEX,
                LumoUtility.JustifyContent.BETWEEN,
                LumoUtility.Padding.SMALL,
                LumoUtility.FontSize.LARGE
        );
        return item;
    }

    private H2 createDayH2() {
        final var day = new H2();
        day.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.Border.BOTTOM,
                LumoUtility.BorderColor.CONTRAST_10,
                LumoUtility.FontSize.XXLARGE,
                Layout.TOP_0,
                LumoUtility.Margin.Bottom.NONE,
                LumoUtility.Margin.Horizontal.AUTO,
                LumoUtility.Margin.Top.MEDIUM,
                LumoUtility.MaxWidth.SCREEN_SMALL,
                LumoUtility.Padding.SMALL,
                LumoUtility.Position.STICKY,
                Layout.Z_10
        );
        return day;
    }

    private static UnorderedList createUnorderedList() {
        var list = new UnorderedList();
        list.addClassNames(
                FontFamily.MONO,
                LumoUtility.ListStyleType.NONE,
                LumoUtility.Margin.Horizontal.AUTO,
                LumoUtility.Margin.Vertical.NONE,
                LumoUtility.MaxWidth.SCREEN_SMALL,
                LumoUtility.Padding.NONE
        );
        return list;
    }


    private Span createDaySpan() {
        final var daySpan = new Span();
        daySpan.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.CENTER, LumoUtility.Padding.MEDIUM, LumoUtility.Background.BASE);
        daySpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL, LumoUtility.Border.BOTTOM, LumoUtility.BorderColor.CONTRAST_10, "sticky-date");
        return daySpan;
    }

    private Div createTimeDiv() {
        final var div = new Div();
        div.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.BETWEEN, LumoUtility.Border.BOTTOM, LumoUtility.BorderColor.CONTRAST_10, LumoUtility.Padding.SMALL, LumoUtility.Padding.Horizontal.MEDIUM);
        return div;
    }


}
