package com.sbar.smsnenado.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.sbar.smsnenado.R;

import static com.sbar.smsnenado.Common.LOGE;
import static com.sbar.smsnenado.Common.LOGI;
import static com.sbar.smsnenado.Common.LOGW;

class BaseActivity extends Activity
{
    private static int sGlobalTheme = R.style.MyTheme_Light;
    public static void setGlobalTheme(int theme)
    {
        sGlobalTheme = theme;
    }

    public void onCreate(Bundle s) {
        updateTheme();
        super.onCreate(s);
    }

    public void updateTheme() {
        SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(this);
        boolean darkTheme = sharedPref.getBoolean(
            SettingsActivity.KEY_BOOL_DARK_THEME,
            false);
        if (darkTheme) {
            setGlobalTheme(R.style.MyTheme_Dark);
        } else {
            setGlobalTheme(R.style.MyTheme_Light);
        }

        setTheme(sGlobalTheme);
    }

    protected void updateOptionsMenu(Menu menu) {
        try {
            int[] menuIcons = null;
            switch (sGlobalTheme) {
            case R.style.MyTheme_Dark:
                menuIcons = sDarkMenuIcons;
                break;
            case R.style.MyTheme_Light:
            default:
                menuIcons = sLightMenuIcons;
                break;
            }

            updateMenuItemIcon(
                menu,
                R.id.about_MenuItem,
                menuIcons[0]
            );

            updateMenuItemIcon(
                menu,
                R.id.search_MenuItem,
                menuIcons[1]
            );

            updateMenuItemIcon(
                menu,
                R.id.settings_MenuItem,
                menuIcons[2]
            );
        } catch (Exception e) {
            LOGE("updateOptionsMenu: " + e.getMessage());
        }
    }

    private void updateMenuItemIcon(Menu menu, int menuItemId, int iconId) {
        MenuItem item = menu.findItem(menuItemId);
        item.setIcon(iconId);
    }

    private static int[] sDarkMenuIcons = {
        R.drawable.ic_action_about_dark,
        R.drawable.ic_action_search_dark,
        R.drawable.ic_action_settings_dark
    };

    private static int[] sLightMenuIcons = {
        R.drawable.ic_action_about,
        R.drawable.ic_action_search,
        R.drawable.ic_action_settings
    };
}
