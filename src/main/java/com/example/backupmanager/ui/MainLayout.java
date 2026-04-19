package com.example.backupmanager.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout {

    public MainLayout() {
        DrawerToggle drawerToggle = new DrawerToggle();
        H1 heading = new H1("PostgreSQL backups");
        heading.getStyle().set("font-size", "var(--lumo-font-size-l)");
        HorizontalLayout navigationBar = new HorizontalLayout(drawerToggle, heading);
        navigationBar.setWidthFull();
        navigationBar.expand(heading);
        addToNavbar(navigationBar);
        VerticalLayout drawerMenu = new VerticalLayout(
                new RouterLink("Backups", BackupView.class),
                new RouterLink("Settings", SettingsView.class));
        drawerMenu.setSizeUndefined();
        addToDrawer(drawerMenu);
    }
}
