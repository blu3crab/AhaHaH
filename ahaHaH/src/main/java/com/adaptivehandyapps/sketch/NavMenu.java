/*
 * Project: AhaThing1
 * Contributor(s): M.A.Tucker, Adaptive Handy Apps, LLC
 * Origination: M.A.Tucker APR 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adaptivehandyapps.sketch;

import android.content.Context;
import android.support.design.widget.NavigationView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import com.adaptivehandyapps.ahahah.R;

import java.util.Arrays;
import java.util.List;

///////////////////////////////////////////////////////////////////////////
// NavMenu: build Nav menu & submenus
public class NavMenu {
    private static final String TAG = "NavMenu";

    private Context mContext;
    private NavigationView mNavigationView;

    ///////////////////////////////////////////////////////////////////////////
    public NavMenu(Context context, NavigationView navigationView) {
        setContext(context);
        mNavigationView = navigationView;
        build(mNavigationView);
    }

    ///////////////////////////////////////////////////////////////////////////
    // getters/setters
    public Context getContext() {
        return mContext;
    }
    public void setContext(Context context) { this.mContext = context; }

    ///////////////////////////////////////////////////////////////////////////
    // build nav menu
    public Boolean build(NavigationView navigationView) {
        // dereference menu & clear any contents
        Menu menu = navigationView.getMenu();
        menu.clear();
//        // menu item defs
//        MenuItem menuItem;
//        int iconId;
//        String itemName;
//        // icon & menu item
//        int iconId = R.drawable.ic_star_black_48dp;
//        // camera
//        iconId = android.R.drawable.ic_menu_camera;
//        itemName = getContext().getString(R.string.action_camera);
//
//        menuItem = menu.add(itemName);
//        menuItem.setIcon(iconId);
//        Log.d(TAG, "build add menu item:" + menuItem.getItemId() + ", itemname: " + menuItem.toString());
//        // gallery
//        iconId = android.R.drawable.ic_menu_gallery;
//        itemName = getContext().getString(R.string.action_gallery);
//
//        menuItem = menu.add(itemName);
//        menuItem.setIcon(iconId);
//        Log.d(TAG, "build add menu item:" + menuItem.getItemId() + ", itemname: " + menuItem.toString());
//
        String title;
        int iconId;
        List<String> monikerList;
        // add file submenu
        title = getContext().getString(R.string.action_sketch_file);
        iconId = android.R.drawable.ic_menu_gallery;
        monikerList = Arrays.asList(getContext().getResources().getStringArray(R.array.File));
        addSubMenu(navigationView, title, iconId, monikerList);
        // add erase submenu
        title = getContext().getString(R.string.action_sketch_erase);
        iconId = R.drawable.ic_undo_black_48dp;
        monikerList = Arrays.asList(getContext().getResources().getStringArray(R.array.Erase));
        addSubMenu(navigationView, title, iconId, monikerList);
        // add shapes submenu
        title = getContext().getString(R.string.action_sketch_shape);
        iconId = R.drawable.ic_bubble_chart_black_48dp;
        monikerList = Arrays.asList(getContext().getResources().getStringArray(R.array.Shapes));
        addSubMenu(navigationView, title, iconId, monikerList);
        // add styles submenu
        title = getContext().getString(R.string.action_sketch_style);
        iconId = R.drawable.ic_border_color_black_48dp;
        monikerList = Arrays.asList(getContext().getResources().getStringArray(R.array.Styles));
        addSubMenu(navigationView, title, iconId, monikerList);
        // add colors submenu
        title = getContext().getString(R.string.action_sketch_color);
        iconId = R.drawable.ic_format_color_fill_black_48dp;
        monikerList = Arrays.asList(getContext().getResources().getStringArray(R.array.Colors));
        addSubMenu(navigationView, title, iconId, monikerList);
        // add tools submenu
        title = getContext().getString(R.string.action_sketch_tool);
        iconId = R.drawable.ic_format_paint_black_48dp;
        monikerList = Arrays.asList(getContext().getResources().getStringArray(R.array.Tools));
        addSubMenu(navigationView, title, iconId, monikerList);

        return true;
    }
    ///////////////////////////////////////////////////////////////////////////
    // add nav sub menu
    private SubMenu addSubMenu(NavigationView navigationView, String title, int iconId, List<String> monikerList) {
        // add submenu from moniker list plus a "new" item
        Menu menu = navigationView.getMenu();
        SubMenu subMenu = menu.addSubMenu(title);
        subMenu.clear();
        MenuItem subMenuItem;
        for (String moniker : monikerList) {
            subMenuItem = subMenu.add(moniker);
            subMenuItem.setIcon(iconId);
//            Log.d(TAG, "addSubMenu submenu item:" + subMenuItem.getItemId() + ", itemname: " + subMenuItem.toString());
        }
        return subMenu;
    }
    ///////////////////////////////////////////////////////////////////////////

}
