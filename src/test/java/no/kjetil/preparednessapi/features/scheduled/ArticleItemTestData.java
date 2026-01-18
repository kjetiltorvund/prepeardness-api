package no.kjetil.preparednessapi.features.scheduled;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;

import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class ArticleItemTestData {
    private static final String[] NAME = {
            "Melk","Brød","Egg","Ost","Yoghurt","Epler","Bananer","Kylling",
            "Tomater","Smør","Poteter","Gulrøtter","Knekkebrød","Rømme",
            "Laks","Skinke","Kjøttdeig","Agurk","Paprika","Sukker"
    };

    private static final String[] PLACEMENT = {
            "Kjøleskap","Fryser","Tørrvarehylla","Fruktfat","Speiskammers"
    };

    private ArticleItemTestData() {}

    public static ArticleItem randomExpired() {
        return randomExpiredBetweenDaysAgo(1, 180);
    }

    public static ArticleItem randomExpiredBetweenDaysAgo(int minDaysAgo, int maxDaysAgo) {
        Instant now = Instant.now();

        DateTime dateTimeNow = now.toDateTime(DateTimeZone.UTC);

        DateTime daysAgo = dateTimeNow.minusDays(ThreadLocalRandom.current().nextInt(minDaysAgo, maxDaysAgo));

        String name = NAME[ThreadLocalRandom.current().nextInt(NAME.length)];
        String placement = PLACEMENT[ThreadLocalRandom.current().nextInt(PLACEMENT.length)];
        String barcode = "EAN" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
        String qr = "QR" + ThreadLocalRandom.current().nextLong(1_000_000L, 9_999_999L);

        return ArticleItem.builder()
                .articleName(name)
                .expirationDate(daysAgo.toDate())
                .expired(true)
                .barcode(barcode)
                .qrCode(qr)
                .active(true)
                .placement(placement)
                .build();
    }

    public static List<ArticleItem> randomExpiredList(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> randomExpiredBetweenDaysAgo(1, 365))
                .toList();
    }
}
