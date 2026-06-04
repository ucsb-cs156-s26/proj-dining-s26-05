import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { http, HttpResponse } from "msw";

import DeveloperPage from "main/pages/DeveloperPage";

export default {
  title: "pages/DeveloperPage",
  component: DeveloperPage,
};

const Template = () => <DeveloperPage />;

export const LoggedInAdminUser = Template.bind({});
LoggedInAdminUser.parameters = {
  msw: {
    handlers: [
      http.get("/api/currentUser", () => {
        return HttpResponse.json(apiCurrentUserFixtures.adminUser);
      }),
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.showingWithRepoInfo);
      }),
    ],
  },
};
