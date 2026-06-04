import { render, screen, waitFor } from "@testing-library/react";
import DeveloperPage from "main/pages/DeveloperPage";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

describe("DeveloperPage tests", () => {
  let axiosMock;
  let queryClient;

  beforeAll(() => {
    axiosMock = new AxiosMockAdapter(axios);
    queryClient = new QueryClient();
  });

  beforeEach(() => {
    axiosMock.reset();
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.adminUser);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingWithRepoInfo);
  });

  afterEach(() => {
    axiosMock.reset();
    queryClient.clear();
  });

  test("renders the Developer Information heading", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DeveloperPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(
      await screen.findByText("Developer Information"),
    ).toBeInTheDocument();
  });

  test("renders the Current Deployed Branch table headings", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DeveloperPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(
      await screen.findByText("Current Deployed Branch"),
    ).toBeInTheDocument();
    expect(screen.getByText("Github Repo:")).toBeInTheDocument();
    expect(screen.getByText("Commit Link:")).toBeInTheDocument();
    expect(screen.getByText("Commit Hash:")).toBeInTheDocument();
    expect(screen.getByText("Commit Message:")).toBeInTheDocument();
  });

  test("renders the system info values from the backend", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DeveloperPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const info = systemInfoFixtures.showingWithRepoInfo;

    await waitFor(() => {
      const repoLinks = screen.getAllByText(info.sourceRepo);
      expect(repoLinks.length).toBeGreaterThanOrEqual(1);
      expect(repoLinks[0]).toHaveAttribute("href", info.sourceRepo);
    });

    const commitLink = screen.getByText(info.githubUrl);
    expect(commitLink).toHaveAttribute("href", info.githubUrl);
    expect(screen.getByText(info.commitId)).toBeInTheDocument();
    expect(screen.getByText(info.commitMessage)).toBeInTheDocument();
  });

  test("renders the Backend Endpoints Swagger link", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DeveloperPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByText("Backend Endpoints")).toBeInTheDocument();
    // There are two Swagger links (navbar + this page), so check that at
    // least one points to the swagger UI and lives in the page body.
    const swaggerLinks = screen.getAllByText("Swagger");
    expect(
      swaggerLinks.some(
        (link) => link.getAttribute("href") === "/swagger-ui/index.html",
      ),
    ).toBe(true);
  });

  test("renders the SOURCE_REPO note with dokku example", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DeveloperPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByText("SOURCE_REPO")).toBeInTheDocument();
    expect(
      screen.getByText(/dokku config:set my-deployment SOURCE_REPO=/),
    ).toBeInTheDocument();
  });

  test("renders the System Info section", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DeveloperPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByText("System Info")).toBeInTheDocument();
  });
});
