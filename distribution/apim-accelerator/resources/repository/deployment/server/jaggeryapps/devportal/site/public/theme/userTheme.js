const Configurations = {
    /* Refer devportal/source/src/defaultTheme.js */
    custom: {
        title: {
            prefix: '[Devportal]',
            sufix: ' WSO2 OH',
        },
        leftMenu: {
            sandboxKeysVisible: true,
            apiKeyVisible: true,
            consentMgtVisible: true,
        },
        apiDetailPages: {
            showSdks: true,
            showDocuments: true,
            showComments: true,
            restrictTryItForAnonymousUsers: true
        },
        appBar: {
            logo: '/site/public/images/OHLogoDevportal.svg', // You can set the url to an external image also ( ex: https://dummyimage.com/208x19/66aad1/ffffff&text=testlogo)
            logoHeight: 35,
            logoWidth: 292,
            background: '#02255C',
            backgroundImage: '/site/public/images/appbarBack.png',
            searchInputBackground: '#ececec',
            searchInputActiveBackground: '#ececec',
            activeBackground: '#7277c3',
            showSearch: true,
            drawerWidth: 200,
            showSwitchDevPortalsBtn: false
        },
        footer: {
            active: true,
            footerHTML: '',
            text: '', // Leave empty to show the default WSO2 Text. Provide custom text to display your own thing.
            background: '#d5d5d5',
            color: '#000',
            height: 50,
        },
        publicTenantStore: {
            disableTenantList: true
        },
    }
};
