package labs.nz1.freetv;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextClock;

public class MainActivity extends Activity {

    private static final class Tile {
        final String label, emoji;
        final String[] candidates;   // package names tried in order (first installed wins)
        final String playId;         // canonical Play Store id for the direct install-page fallback
        Tile(String l, String e, String[] c, String p) { label = l; emoji = e; candidates = c; playId = p; }
    }

    private static final Tile[] TILES = {
        new Tile("Tubi",       "🎬", new String[]{"com.tubitv"}, "com.tubitv"),
        new Tile("Plex",       "🍿", new String[]{"com.plexapp.android"}, "com.plexapp.android"),
        // ThreeNow: use the NATIVE TV build (.tv) — the mobile build self-exits on
        // generic boxes; the TV build (preinstalled on this unit) launches and holds.
        new Tile("ThreeNow",   "🥝", new String[]{"com.mediaworks.android.tv", "com.mediaworks.android"}, null),
        // TVNZ+ removed 2026-08-25: preinstalled package has NO launchable activity
        // (firmware stub) — tile was dead weight. Maori+ removed: unreliable UX per user.
        // Freeview removed 2026-08-25: TV build abandoned by vendor, non-functional on
        // generic boxes. App left installed on box; suppressed via SEEDED below.
        new Tile("YouTube",    "▶\uFE0F", new String[]{"com.google.android.youtube.tv"}, null),
        new Tile("Kodi",       "\uD83C\uDF9E\uFE0F", new String[]{"org.xbmc.kodi"}, null),
        new Tile("Play Store", "\uD83D\uDED2", new String[]{"com.android.vending"}, null),
        new Tile("Settings",   "⚙\uFE0F", null, null),
    };

    private static final int ACCENT   = Color.parseColor("#35E0FF");
    private static final int CARD_TOP = Color.parseColor("#17334F");
    private static final int CARD_BOT = Color.parseColor("#0B1726");

    private View mHomeTile; // first tile, re-focused whenever we return to the foreground

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUi();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        // TV-safe-area margins keep focused-tile glow inside the visible frame.
        root.setPadding(px(46), px(14), px(46), px(8));

        // ---- Header: title + live clock ------------------------------------
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("NZ1 FREE TV");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, tp);

        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("EEE d MMM  h:mm a");
        clock.setFormat24Hour("EEE d MMM  HH:mm");
        clock.setTextColor(Color.parseColor("#8FA3B8"));
        clock.setTextSize(15);
        header.addView(clock);

        root.addView(header);

        // ---- Tile grid ------------------------------------------------------
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setUseDefaultMargins(true);
        int topPad = px(10);
        grid.setPadding(0, topPad, 0, 0);

        java.util.LinkedHashMap<String, CharSequence> installed = getInstalledApps();
        boolean first = true;
        for (Tile t : TILES) {
            View cell = buildCell(t);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f));
            lp.height = px(96);
            grid.addView(cell, lp);
            if (first) { mHomeTile = cell; cell.requestFocus(); first = false; }
        }
        for (final String pkg : installed.keySet()) {
            View cell = buildAppCell(pkg, installed.get(pkg));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f));
            lp.height = px(96);
            grid.addView(cell, lp);
        }
        root.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ---- Footer ---------------------------------------------------------
        TextView foot = new TextView(this);
        foot.setText("D-pad move · OK open · Back stays here · Hold Home any time to switch launchers");
        foot.setTextColor(Color.parseColor("#5A6B7D"));
        foot.setTextSize(12);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(foot, fp);

        setContentView(root);
    }

    private View buildCell(final Tile t) {
        FrameLayout cell = new FrameLayout(this);
        cell.setFocusable(true);
        cell.setFocusableInTouchMode(true);
        cell.setClickable(true);

        GradientDrawable bg = cardBg(false);
        cell.setBackground(bg);
        cell.setElevation(px(4));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        cell.addView(inner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView emoji = new TextView(this);
        emoji.setText(t.emoji);
        emoji.setTextSize(30);
        emoji.setGravity(Gravity.CENTER);
        inner.addView(emoji);

        TextView label = new TextView(this);
        label.setText(t.label);
        label.setTextColor(Color.WHITE);
        label.setTextSize(17);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, px(3), 0, 0);
        inner.addView(label);

        cell.setOnFocusChangeListener((v, has) -> {
            v.setBackground(cardBg(has));
            float s = has ? 1.03f : 1.0f;
            v.animate().scaleX(s).scaleY(s).setDuration(120).start();
            v.setElevation(has ? px(10) : px(4));
        });

        cell.setOnClickListener(v -> launch(t));
        return cell;
    }

    /** User-installed apps (not pre-seeded tiles), newest first, capped so the grid stays clean. */
    private java.util.LinkedHashMap<String, CharSequence> getInstalledApps() {
        java.util.LinkedHashMap<String, CharSequence> out = new java.util.LinkedHashMap<>();
        PackageManager pm = getPackageManager();
        java.util.List<android.content.pm.PackageInfo> pkgs = pm.getInstalledPackages(0);
        // Newest install first.
        java.util.Collections.sort(pkgs, (a, b) -> Long.compare(b.firstInstallTime, a.firstInstallTime));
        int added = 0;
        for (android.content.pm.PackageInfo pi : pkgs) {
            String pkg = pi.packageName;
            if (SEEDED.contains(pkg)) continue;                 // already has its own tile
            Intent li = pm.getLaunchIntentForPackage(pkg);
            if (li == null || li.getComponent() == null) continue; // no UI -> skip
            String lpkg = li.getComponent().getPackageName();
            if (lpkg.startsWith("com.android.") || lpkg.startsWith("com.google.android.gms")) continue;
            CharSequence label = pi.applicationInfo != null ? pi.applicationInfo.loadLabel(pm) : pkg;
            out.put(pkg, label);
            if (++added >= 2) break;
        }
        return out;
    }

    private static final java.util.Set<String> SEEDED = new java.util.HashSet<>(java.util.Arrays.asList(
            "labs.nz1.freetv", "com.tubitv", "com.plexapp.android",
            "com.mediaworks.android.tv", "com.mediaworks.android",
            "nz.co.dishtv.FreeviewLiveTV",   // installed but stripped — never auto-tile it
            "com.google.android.youtube.tv", "org.xbmc.kodi",
            "com.android.vending"));

    private View buildAppCell(final String pkg, final CharSequence label) {
        FrameLayout cell = new FrameLayout(this);
        cell.setFocusable(true);
        cell.setFocusableInTouchMode(true);
        cell.setClickable(true);
        GradientDrawable bg = cardBg(false);
        cell.setBackground(bg);
        cell.setElevation(px(4));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        cell.addView(inner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView emoji = new TextView(this);
        emoji.setText("\uD83D\uDCFD"); // 📝-style generic TV mark: 📝 replaced with 📽 family glyph
        emoji.setTextSize(30);
        emoji.setGravity(Gravity.CENTER);
        inner.addView(emoji);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(17);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, px(3), 0, 0);
        tv.setMaxLines(1);
        inner.addView(tv);

        cell.setOnFocusChangeListener((v, has) -> {
            v.setBackground(cardBg(has));
            float s = has ? 1.03f : 1.0f;
            v.animate().scaleX(s).scaleY(s).setDuration(120).start();
            v.setElevation(has ? px(10) : px(4));
        });
        cell.setOnClickListener(v -> launchApp(pkg));
        return cell;
    }

    private void launchApp(String pkg) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
        } catch (Exception e) { toast("Can't open " + pkg); }
    }

    private GradientDrawable cardBg(boolean focused) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                focused
                        ? new int[]{Color.parseColor("#1E4066"), CARD_TOP}
                        : new int[]{CARD_TOP, CARD_BOT});
        g.setCornerRadius(px(20));
        g.setStroke(focused ? px(3) : px(1), focused ? ACCENT : Color.parseColor("#22364F"));
        return g;
    }

    private void launch(Tile t) {
        if (t.candidates == null) { // Settings tile
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (Exception e) { toast("Settings unavailable"); }
            return;
        }
        for (String pkg : t.candidates) {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                try { startActivity(i); return; } catch (Exception ignored) {}
            }
        }
        // Not installed -> open the app's own Play Store page (Install button front and center).
        if (t.playId != null && !isInstalled(t.playId)) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + t.playId)));
                toast("Hit Install on " + t.label + ", then use its tile again");
            } catch (Exception e2) {
                try { startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + t.playId))); }
                catch (Exception e3) { toast(t.label + " not available on Play Store"); }
            }
        } else if (!"Play Store".equals(t.label)) {
            toast(t.label + " isn't installed");
        }
    }

    private boolean isInstalled(String pkg) {
        try { getPackageManager().getPackageInfo(pkg, 0); return true; }
        catch (Exception e) { return false; }
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }

    // We are HOME — swallowing Back avoids flashing to the old launcher and back.
    @Override
    public void onBackPressed() { }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUi();
            // Returning from another app can leave zero focus -> dead remote.
            if (getCurrentFocus() == null && mHomeTile != null) mHomeTile.requestFocus();
        }
    }

    private void hideSystemUi() {
        View d = getWindow().getDecorView();
        d.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private int px(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
